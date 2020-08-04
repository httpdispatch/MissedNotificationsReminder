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
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.app.missednotificationsreminder.BuildConfig;
import com.app.missednotificationsreminder.R;
import com.app.missednotificationsreminder.databinding.DebugViewContentBinding;
import com.app.missednotificationsreminder.di.Injector;
import com.f2prateek.rx.preferences.Preference;
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

import javax.inject.Inject;

import timber.log.Timber;

import static org.threeten.bp.format.DateTimeFormatter.ISO_INSTANT;

public final class DebugView extends FrameLayout {
  private static final DateTimeFormatter DATE_DISPLAY_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a", Locale.US).withZone(ZoneId.systemDefault());


  DebugViewContentBinding mBinding;
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
    mBinding = DebugViewContentBinding.inflate(LayoutInflater.from(context), this, true);

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
    mBinding.debugUiAnimationSpeed.setAdapter(speedAdapter);
    final int animationSpeedValue = animationSpeed.get();
    mBinding.debugUiAnimationSpeed.setSelection(
        AnimationSpeedAdapter.getPositionForValue(animationSpeedValue));

    mBinding.debugUiAnimationSpeed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int selected = speedAdapter.getItem(position);
        if(selected != animationSpeed.get()){
          Timber.d("Setting animation speed to %sx", selected);
          animationSpeed.set(selected);
          applyAnimationSpeed(selected);
        }
      }

      @Override public void onNothingSelected(AdapterView<?> parent) {

      }
    });
    // Ensure the animation speed value is always applied across app restarts.
    post(() -> applyAnimationSpeed(animationSpeedValue));

    boolean gridEnabled = pixelGridEnabled.get();
    mBinding.debugUiPixelGrid.setChecked(gridEnabled);
    mBinding.debugUiPixelRatio.setEnabled(gridEnabled);
    mBinding.debugUiPixelGrid.setOnCheckedChangeListener((buttonView, isChecked) -> {
      Timber.d("Setting pixel grid overlay enabled to " + isChecked);
      pixelGridEnabled.set(isChecked);
      mBinding.debugUiPixelRatio.setEnabled(isChecked);
    });

    mBinding.debugUiPixelRatio.setChecked(pixelRatioEnabled.get());
    mBinding.debugUiPixelRatio.setOnCheckedChangeListener((buttonView, isChecked) -> {
      Timber.d("Setting pixel scale overlay enabled to " + isChecked);
      pixelRatioEnabled.set(isChecked);
    });

    mBinding.debugUiScalpel.setChecked(scalpelEnabled.get());
    mBinding.debugUiScalpelWireframe.setEnabled(scalpelEnabled.get());
    mBinding.debugUiScalpel.setOnCheckedChangeListener((buttonView, isChecked) -> {
      Timber.d("Setting scalpel interaction enabled to " + isChecked);
      scalpelEnabled.set(isChecked);
      mBinding.debugUiScalpelWireframe.setEnabled(isChecked);
    });

    mBinding.debugUiScalpelWireframe.setChecked(scalpelWireframeEnabled.get());
    mBinding.debugUiScalpelWireframe.setOnCheckedChangeListener((buttonView, isChecked) -> {
      Timber.d("Setting scalpel wireframe enabled to " + isChecked);
      scalpelWireframeEnabled.set(isChecked);
    });
    mBinding.debugLogsShow.setOnClickListener(view -> new LogsDialog(new ContextThemeWrapper(getContext(), R.style.AppTheme), lumberYard).show());
  }

  private void setupBuildSection() {
    mBinding.debugBuildName.setText(BuildConfig.VERSION_NAME);
    mBinding.debugBuildCode.setText(String.valueOf(BuildConfig.VERSION_CODE));
    mBinding.debugBuildSha.setText(BuildConfig.GIT_SHA);

    TemporalAccessor buildTime = ISO_INSTANT.parse(BuildConfig.BUILD_TIME);
    mBinding.debugBuildDate.setText(DATE_DISPLAY_FORMAT.format(buildTime));
  }

  private void setupDeviceSection() {
    DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
    String densityBucket = getDensityString(displayMetrics);
    mBinding.debugDeviceMake.setText(Strings.truncateAt(Build.MANUFACTURER, 20));
    mBinding.debugDeviceModel.setText(Strings.truncateAt(Build.MODEL, 20));
    mBinding.debugDeviceResolution.setText(displayMetrics.heightPixels + "x" + displayMetrics.widthPixels);
    mBinding.debugDeviceDensity.setText(displayMetrics.densityDpi + "dpi (" + densityBucket + ")");
    mBinding.debugDeviceRelease.setText(Build.VERSION.RELEASE);
    mBinding.debugDeviceApi.setText(String.valueOf(Build.VERSION.SDK_INT));
  }

  private void setupPicassoSection() {
    boolean picassoDebuggingValue = picassoDebugging.get();
    picasso.setIndicatorsEnabled(picassoDebuggingValue);
    mBinding.debugPicassoIndicators.setChecked(picassoDebuggingValue);
    mBinding.debugPicassoIndicators.setOnCheckedChangeListener((button, isChecked) -> {
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
    mBinding.debugPicassoCacheSize.setText(size + " / " + total + " (" + percentage + "%)");
    mBinding.debugPicassoCacheHit.setText(String.valueOf(snapshot.cacheHits));
    mBinding.debugPicassoCacheMiss.setText(String.valueOf(snapshot.cacheMisses));
    mBinding.debugPicassoDecoded.setText(String.valueOf(snapshot.originalBitmapCount));
    mBinding.debugPicassoDecodedTotal.setText(getSizeString(snapshot.totalOriginalBitmapSize));
    mBinding.debugPicassoDecodedAvg.setText(getSizeString(snapshot.averageOriginalBitmapSize));
    mBinding.debugPicassoTransformed.setText(String.valueOf(snapshot.transformedBitmapCount));
    mBinding.debugPicassoTransformedTotal.setText(getSizeString(snapshot.totalTransformedBitmapSize));
    mBinding.debugPicassoTransformedAvg.setText(getSizeString(snapshot.averageTransformedBitmapSize));
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
