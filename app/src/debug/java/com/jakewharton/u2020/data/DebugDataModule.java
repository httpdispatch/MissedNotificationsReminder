package com.jakewharton.u2020.data;

import com.f2prateek.rx.preferences.Preference;
import com.f2prateek.rx.preferences.RxSharedPreferences;
import com.jakewharton.u2020.data.AnimationSpeed;
import com.jakewharton.u2020.data.PicassoDebugging;
import com.jakewharton.u2020.data.PixelGridEnabled;
import com.jakewharton.u2020.data.PixelRatioEnabled;
import com.jakewharton.u2020.data.ScalpelEnabled;
import com.jakewharton.u2020.data.ScalpelWireframeEnabled;
import com.jakewharton.u2020.data.SeenDebugDrawer;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        complete = false,
        library = true,
        overrides = true
)
public final class DebugDataModule {
        private static final int DEFAULT_ANIMATION_SPEED = 1; // 1x (normal) speed.
        private static final boolean DEFAULT_PICASSO_DEBUGGING = false; // Debug indicators displayed
        private static final boolean DEFAULT_PIXEL_GRID_ENABLED = false; // No pixel grid overlay.
        private static final boolean DEFAULT_PIXEL_RATIO_ENABLED = false; // No pixel ratio overlay.
        private static final boolean DEFAULT_SCALPEL_ENABLED = false; // No crazy 3D view tree.
        private static final boolean DEFAULT_SCALPEL_WIREFRAME_ENABLED = false; // Draw views by default.
        private static final boolean DEFAULT_SEEN_DEBUG_DRAWER = false; // Show debug drawer first time.

        @Provides @Singleton @AnimationSpeed Preference<Integer> provideAnimationSpeed(RxSharedPreferences preferences) {
                return preferences.getInteger("debug_animation_speed", DEFAULT_ANIMATION_SPEED);
        }

        @Provides @Singleton @PicassoDebugging
        Preference<Boolean> providePicassoDebugging(RxSharedPreferences preferences) {
                return preferences.getBoolean("debug_picasso_debugging", DEFAULT_PICASSO_DEBUGGING);
        }

        @Provides @Singleton @PixelGridEnabled
        Preference<Boolean> providePixelGridEnabled(RxSharedPreferences preferences) {
                return preferences.getBoolean("debug_pixel_grid_enabled", DEFAULT_PIXEL_GRID_ENABLED);
        }

        @Provides @Singleton @PixelRatioEnabled
        Preference<Boolean> providePixelRatioEnabled(RxSharedPreferences preferences) {
                return preferences.getBoolean("debug_pixel_ratio_enabled", DEFAULT_PIXEL_RATIO_ENABLED);
        }

        @Provides @Singleton @SeenDebugDrawer
        Preference<Boolean> provideSeenDebugDrawer(RxSharedPreferences preferences) {
                return preferences.getBoolean("debug_seen_debug_drawer", DEFAULT_SEEN_DEBUG_DRAWER);
        }

        @Provides @Singleton @ScalpelEnabled
        Preference<Boolean> provideScalpelEnabled(RxSharedPreferences preferences) {
                return preferences.getBoolean("debug_scalpel_enabled", DEFAULT_SCALPEL_ENABLED);
        }

        @Provides @Singleton @ScalpelWireframeEnabled
        Preference<Boolean> provideScalpelWireframeEnabled(RxSharedPreferences preferences) {
                return preferences.getBoolean("debug_scalpel_wireframe_drawer",
                        DEFAULT_SCALPEL_WIREFRAME_ENABLED);
        }
}
