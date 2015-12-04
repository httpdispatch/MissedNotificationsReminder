package com.jakewharton.u2020.ui.debug;

import android.animation.ValueAnimator;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.app.missednotificationsreminder.BuildConfig;
import com.app.missednotificationsreminder.R;
import com.app.missednotificationsreminder.di.Injector;
import com.f2prateek.rx.preferences.Preference;
import com.jakewharton.rxbinding.widget.RxAdapterView;
import com.jakewharton.u2020.data.AnimationSpeed;
import com.jakewharton.u2020.data.LumberYard;
import com.jakewharton.u2020.data.PicassoDebugging;
import com.jakewharton.u2020.data.PixelGridEnabled;
import com.jakewharton.u2020.data.PixelRatioEnabled;
import com.jakewharton.u2020.data.ScalpelEnabled;
import com.jakewharton.u2020.data.ScalpelWireframeEnabled;
import com.jakewharton.u2020.ui.logs.LogsDialog;
import com.jakewharton.u2020.util.Strings;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.StatsSnapshot;

import org.threeten.bp.ZoneId;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.temporal.TemporalAccessor;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

import static org.threeten.bp.format.DateTimeFormatter.ISO_INSTANT;

public final class DebugView extends FrameLayout {
  private static final DateTimeFormatter DATE_DISPLAY_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a", Locale.US).withZone(ZoneId.systemDefault());

  @Bind(R.id.debug_contextual_title) View contextualTitleView;
  @Bind(R.id.debug_contextual_list) LinearLayout contextualListView;

  @Bind(R.id.debug_ui_animation_speed) Spinner uiAnimationSpeedView;
  @Bind(R.id.debug_ui_pixel_grid) Switch uiPixelGridView;
  @Bind(R.id.debug_ui_pixel_ratio) Switch uiPixelRatioView;
  @Bind(R.id.debug_ui_scalpel) Switch uiScalpelView;
  @Bind(R.id.debug_ui_scalpel_wireframe) Switch uiScalpelWireframeView;

  @Bind(R.id.debug_build_name) TextView buildNameView;
  @Bind(R.id.debug_build_code) TextView buildCodeView;
  @Bind(R.id.debug_build_sha) TextView buildShaView;
  @Bind(R.id.debug_build_date) TextView buildDateView;

  @Bind(R.id.debug_device_make) TextView deviceMakeView;
  @Bind(R.id.debug_device_model) TextView deviceModelView;
  @Bind(R.id.debug_device_resolution) TextView deviceResolutionView;
  @Bind(R.id.debug_device_density) TextView deviceDensityView;
  @Bind(R.id.debug_device_release) TextView deviceReleaseView;
  @Bind(R.id.debug_device_api) TextView deviceApiView;

  @Bind(R.id.debug_picasso_indicators) Switch picassoIndicatorView;
  @Bind(R.id.debug_picasso_cache_size) TextView picassoCacheSizeView;
  @Bind(R.id.debug_picasso_cache_hit) TextView picassoCacheHitView;
  @Bind(R.id.debug_picasso_cache_miss) TextView picassoCacheMissView;
  @Bind(R.id.debug_picasso_decoded) TextView picassoDecodedView;
  @Bind(R.id.debug_picasso_decoded_total) TextView picassoDecodedTotalView;
  @Bind(R.id.debug_picasso_decoded_avg) TextView picassoDecodedAvgView;
  @Bind(R.id.debug_picasso_transformed) TextView picassoTransformedView;
  @Bind(R.id.debug_picasso_transformed_total) TextView picassoTransformedTotalView;
  @Bind(R.id.debug_picasso_transformed_avg) TextView picassoTransformedAvgView;

  @Inject Picasso picasso;
  @Inject LumberYard lumberYard;
  @Inject @AnimationSpeed Preference<Integer> animationSpeed;
  @Inject @PicassoDebugging Preference<Boolean> picassoDebugging;
  @Inject @PixelGridEnabled Preference<Boolean> pixelGridEnabled;
  @Inject @PixelRatioEnabled Preference<Boolean> pixelRatioEnabled;
  @Inject @ScalpelEnabled Preference<Boolean> scalpelEnabled;
  @Inject @ScalpelWireframeEnabled Preference<Boolean> scalpelWireframeEnabled;
  @Inject Application app;

  public DebugView(Context context) {
    this(context, null);
  }

  public DebugView(Context context, AttributeSet attrs) {
    super(context, attrs);
    Injector.obtain(context).inject(this);

    // Inflate all of the controls and inject them.
    LayoutInflater.from(context).inflate(R.layout.debug_view_content, this);
    ButterKnife.bind(this);


    setupUserInterfaceSection();
    setupBuildSection();
    setupDeviceSection();
    setupPicassoSection();
  }

  public void onDrawerOpened() {
    refreshPicassoStats();
  }

  private void setupUserInterfaceSection() {
    final AnimationSpeedAdapter speedAdapter = new AnimationSpeedAdapter(getContext());
    uiAnimationSpeedView.setAdapter(speedAdapter);
    final int animationSpeedValue = animationSpeed.get();
    uiAnimationSpeedView.setSelection(
        AnimationSpeedAdapter.getPositionForValue(animationSpeedValue));

    RxAdapterView.itemSelections(uiAnimationSpeedView)
        .map(speedAdapter::getItem)
        .filter(item -> item != animationSpeed.get())
        .subscribe(selected -> {
          Timber.d("Setting animation speed to %sx", selected);
          animationSpeed.set(selected);
          applyAnimationSpeed(selected);
        });
    // Ensure the animation speed value is always applied across app restarts.
    post(() -> applyAnimationSpeed(animationSpeedValue));

    boolean gridEnabled = pixelGridEnabled.get();
    uiPixelGridView.setChecked(gridEnabled);
    uiPixelRatioView.setEnabled(gridEnabled);
    uiPixelGridView.setOnCheckedChangeListener((buttonView, isChecked) -> {
      Timber.d("Setting pixel grid overlay enabled to " + isChecked);
      pixelGridEnabled.set(isChecked);
      uiPixelRatioView.setEnabled(isChecked);
    });

    uiPixelRatioView.setChecked(pixelRatioEnabled.get());
    uiPixelRatioView.setOnCheckedChangeListener((buttonView, isChecked) -> {
      Timber.d("Setting pixel scale overlay enabled to " + isChecked);
      pixelRatioEnabled.set(isChecked);
    });

    uiScalpelView.setChecked(scalpelEnabled.get());
    uiScalpelWireframeView.setEnabled(scalpelEnabled.get());
    uiScalpelView.setOnCheckedChangeListener((buttonView, isChecked) -> {
      Timber.d("Setting scalpel interaction enabled to " + isChecked);
      scalpelEnabled.set(isChecked);
      uiScalpelWireframeView.setEnabled(isChecked);
    });

    uiScalpelWireframeView.setChecked(scalpelWireframeEnabled.get());
    uiScalpelWireframeView.setOnCheckedChangeListener((buttonView, isChecked) -> {
      Timber.d("Setting scalpel wireframe enabled to " + isChecked);
      scalpelWireframeEnabled.set(isChecked);
    });
  }

  @OnClick(R.id.debug_logs_show) void showLogs() {
    new LogsDialog(new ContextThemeWrapper(getContext(), R.style.AppTheme), lumberYard).show();
  }

  private void setupBuildSection() {
    buildNameView.setText(BuildConfig.VERSION_NAME);
    buildCodeView.setText(String.valueOf(BuildConfig.VERSION_CODE));
    buildShaView.setText(BuildConfig.GIT_SHA);

    TemporalAccessor buildTime = ISO_INSTANT.parse(BuildConfig.BUILD_TIME);
    buildDateView.setText(DATE_DISPLAY_FORMAT.format(buildTime));
  }

  private void setupDeviceSection() {
    DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
    String densityBucket = getDensityString(displayMetrics);
    deviceMakeView.setText(Strings.truncateAt(Build.MANUFACTURER, 20));
    deviceModelView.setText(Strings.truncateAt(Build.MODEL, 20));
    deviceResolutionView.setText(displayMetrics.heightPixels + "x" + displayMetrics.widthPixels);
    deviceDensityView.setText(displayMetrics.densityDpi + "dpi (" + densityBucket + ")");
    deviceReleaseView.setText(Build.VERSION.RELEASE);
    deviceApiView.setText(String.valueOf(Build.VERSION.SDK_INT));
  }

  private void setupPicassoSection() {
    boolean picassoDebuggingValue = picassoDebugging.get();
    picasso.setIndicatorsEnabled(picassoDebuggingValue);
    picassoIndicatorView.setChecked(picassoDebuggingValue);
    picassoIndicatorView.setOnCheckedChangeListener((button, isChecked) -> {
      Timber.d("Setting Picasso debugging to " + isChecked);
      picasso.setIndicatorsEnabled(isChecked);
      picassoDebugging.set(isChecked);
    });

    refreshPicassoStats();
  }

  private void refreshPicassoStats() {
    StatsSnapshot snapshot = picasso.getSnapshot();
    String size = getSizeString(snapshot.size);
    String total = getSizeString(snapshot.maxSize);
    int percentage = (int) ((1f * snapshot.size / snapshot.maxSize) * 100);
    picassoCacheSizeView.setText(size + " / " + total + " (" + percentage + "%)");
    picassoCacheHitView.setText(String.valueOf(snapshot.cacheHits));
    picassoCacheMissView.setText(String.valueOf(snapshot.cacheMisses));
    picassoDecodedView.setText(String.valueOf(snapshot.originalBitmapCount));
    picassoDecodedTotalView.setText(getSizeString(snapshot.totalOriginalBitmapSize));
    picassoDecodedAvgView.setText(getSizeString(snapshot.averageOriginalBitmapSize));
    picassoTransformedView.setText(String.valueOf(snapshot.transformedBitmapCount));
    picassoTransformedTotalView.setText(getSizeString(snapshot.totalTransformedBitmapSize));
    picassoTransformedAvgView.setText(getSizeString(snapshot.averageTransformedBitmapSize));
  }

  private void applyAnimationSpeed(int multiplier) {
    try {
      Method method = ValueAnimator.class.getDeclaredMethod("setDurationScale", float.class);
      method.invoke(null, (float) multiplier);
    } catch (Exception e) {
      Toast.makeText(getContext(), "Unable to apply animation speed. "+e.getMessage(),Toast.LENGTH_LONG).show();
    }
  }

  private static String getDensityString(DisplayMetrics displayMetrics) {
    switch (displayMetrics.densityDpi) {
      case DisplayMetrics.DENSITY_LOW:
        return "ldpi";
      case DisplayMetrics.DENSITY_MEDIUM:
        return "mdpi";
      case DisplayMetrics.DENSITY_HIGH:
        return "hdpi";
      case DisplayMetrics.DENSITY_XHIGH:
        return "xhdpi";
      case DisplayMetrics.DENSITY_XXHIGH:
        return "xxhdpi";
      case DisplayMetrics.DENSITY_XXXHIGH:
        return "xxxhdpi";
      case DisplayMetrics.DENSITY_TV:
        return "tvdpi";
      default:
        return String.valueOf(displayMetrics.densityDpi);
    }
  }

  private static String getSizeString(long bytes) {
    String[] units = new String[] { "B", "KB", "MB", "GB" };
    int unit = 0;
    while (bytes >= 1024) {
      bytes /= 1024;
      unit += 1;
    }
    return bytes + units[unit];
  }
}
