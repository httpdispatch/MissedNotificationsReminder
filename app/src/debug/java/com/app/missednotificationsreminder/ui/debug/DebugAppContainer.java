package com.app.missednotificationsreminder.ui.debug;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.PowerManager;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.app.missednotificationsreminder.R;
import com.app.missednotificationsreminder.databinding.DebugActivityFrameBinding;
import com.app.missednotificationsreminder.ui.AppContainer;
import com.f2prateek.rx.preferences.Preference;
import com.jakewharton.u2020.data.LumberYard;
import com.jakewharton.u2020.data.PixelGridEnabled;
import com.jakewharton.u2020.data.PixelRatioEnabled;
import com.jakewharton.u2020.data.ScalpelEnabled;
import com.jakewharton.u2020.data.ScalpelWireframeEnabled;
import com.jakewharton.u2020.data.SeenDebugDrawer;
import com.jakewharton.u2020.ui.bugreport.BugReportLens;
import com.jakewharton.u2020.ui.debug.DebugDrawerLayout;
import com.jakewharton.u2020.ui.debug.DebugView;
import com.jakewharton.u2020.util.EmptyActivityLifecycleCallbacks;
import com.mattprecious.telescope.TelescopeLayout;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.core.view.GravityCompat;
import rx.subscriptions.CompositeSubscription;

import static android.content.Context.POWER_SERVICE;
import static android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP;
import static android.os.PowerManager.FULL_WAKE_LOCK;
import static android.os.PowerManager.ON_AFTER_RELEASE;
import static android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;

/**
 * An {@link AppContainer} for debug builds which wrap the content view with a sliding drawer on
 * the right that holds all of the debug information and settings.
 */
@Singleton
public final class DebugAppContainer implements AppContainer {
  private final LumberYard lumberYard;
  private final Preference<Boolean> seenDebugDrawer;
  private final Preference<Boolean> pixelGridEnabled;
  private final Preference<Boolean> pixelRatioEnabled;
  private final Preference<Boolean> scalpelEnabled;
  private final Preference<Boolean> scalpelWireframeEnabled;

  @Inject public DebugAppContainer(LumberYard lumberYard,
                                   @SeenDebugDrawer Preference<Boolean> seenDebugDrawer,
                                   @PixelGridEnabled Preference<Boolean> pixelGridEnabled,
                                   @PixelRatioEnabled Preference<Boolean> pixelRatioEnabled,
                                   @ScalpelEnabled Preference<Boolean> scalpelEnabled,
                                   @ScalpelWireframeEnabled Preference<Boolean> scalpelWireframeEnabled) {
    this.lumberYard = lumberYard;
    this.seenDebugDrawer = seenDebugDrawer;
    this.pixelGridEnabled = pixelGridEnabled;
    this.pixelRatioEnabled = pixelRatioEnabled;
    this.scalpelEnabled = scalpelEnabled;
    this.scalpelWireframeEnabled = scalpelWireframeEnabled;
  }

  @Override public ViewGroup bind(final Activity activity) {
    DebugActivityFrameBinding binding = DebugActivityFrameBinding.inflate(activity.getLayoutInflater());
    activity.setContentView(binding.getRoot());

    final Context drawerContext = new ContextThemeWrapper(activity, R.style.Theme_U2020_Debug);
    final DebugView debugView = new DebugView(drawerContext);
    binding.debugDrawer.addView(debugView);

//    binding.drawerLayout.setDrawerShadow(R.drawable.debug_drawer_shadow, GravityCompat.END);
    binding.debugDrawerLayout.setDrawerListener(new DebugDrawerLayout.SimpleDrawerListener() {
      @Override public void onDrawerOpened(View drawerView) {
        debugView.onDrawerOpened();
      }
    });

    TelescopeLayout.cleanUp(activity); // Clean up any old screenshots.
    binding.telescopeContainer.setLens(new BugReportLens(activity, lumberYard));

    // If you have not seen the debug drawer before, show it with a message
    if (!seenDebugDrawer.get()) {
      binding.debugDrawerLayout.postDelayed(() -> {
        binding.debugDrawerLayout.openDrawer(GravityCompat.END);
        Toast.makeText(drawerContext, R.string.debug_drawer_welcome, Toast.LENGTH_LONG).show();
      }, 1000);
      seenDebugDrawer.set(true);
    }

    final CompositeSubscription subscriptions = new CompositeSubscription();
    setupMadge(binding, subscriptions);
    setupScalpel(binding, subscriptions);

    final Application app = activity.getApplication();
    app.registerActivityLifecycleCallbacks(new EmptyActivityLifecycleCallbacks() {
      @Override public void onActivityDestroyed(Activity lifecycleActivity) {
        if (lifecycleActivity == activity) {
          subscriptions.unsubscribe();
          app.unregisterActivityLifecycleCallbacks(this);
        }
      }
    });

    riseAndShine(activity);
    return binding.debugContent;
  }

  private void setupMadge(final DebugActivityFrameBinding binding, CompositeSubscription subscriptions) {
    subscriptions.add(pixelGridEnabled.asObservable().subscribe(enabled -> {
      binding.madgeContainer.setOverlayEnabled(enabled);
    }));
    subscriptions.add(pixelRatioEnabled.asObservable().subscribe(enabled -> {
      binding.madgeContainer.setOverlayRatioEnabled(enabled);
    }));
  }

  private void setupScalpel(final DebugActivityFrameBinding binding, CompositeSubscription subscriptions) {
    subscriptions.add(scalpelEnabled.asObservable().subscribe(enabled -> {
      binding.debugContent.setLayerInteractionEnabled(enabled);
    }));
    subscriptions.add(scalpelWireframeEnabled.asObservable().subscribe(enabled -> {
      binding.debugContent.setDrawViews(!enabled);
    }));
  }

  /**
   * Show the activity over the lock-screen and wake up the device. If you launched the app manually
   * both of these conditions are already true. If you deployed from the IDE, however, this will
   * save you from hundreds of power button presses and pattern swiping per day!
   */
  public static void riseAndShine(Activity activity) {
    activity.getWindow().addFlags(FLAG_SHOW_WHEN_LOCKED);

    PowerManager power = (PowerManager) activity.getSystemService(POWER_SERVICE);
    PowerManager.WakeLock lock =
        power.newWakeLock(FULL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP | ON_AFTER_RELEASE, "mnr:wakeup!");
    lock.acquire();
    lock.release();
  }
}
