package com.jakewharton.u2020.data

import com.tfcporciuncula.flow.FlowSharedPreferences
import com.tfcporciuncula.flow.Preference
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Singleton

@Module
@OptIn(ExperimentalCoroutinesApi::class)
class DebugDataModule {
    @Provides
    @Singleton
    @AnimationSpeed
    fun provideAnimationSpeed(preferences: FlowSharedPreferences): Preference<Int> {
        return preferences.getInt("debug_animation_speed", DEFAULT_ANIMATION_SPEED)
    }

    @Provides
    @Singleton
    @PicassoDebugging
    fun providePicassoDebugging(preferences: FlowSharedPreferences): Preference<Boolean> {
        return preferences.getBoolean("debug_picasso_debugging", DEFAULT_PICASSO_DEBUGGING)
    }

    @Provides
    @Singleton
    @PixelGridEnabled
    fun providePixelGridEnabled(preferences: FlowSharedPreferences): Preference<Boolean> {
        return preferences.getBoolean("debug_pixel_grid_enabled", DEFAULT_PIXEL_GRID_ENABLED)
    }

    @Provides
    @Singleton
    @PixelRatioEnabled
    fun providePixelRatioEnabled(preferences: FlowSharedPreferences): Preference<Boolean> {
        return preferences.getBoolean("debug_pixel_ratio_enabled", DEFAULT_PIXEL_RATIO_ENABLED)
    }

    @Provides
    @Singleton
    @SeenDebugDrawer
    fun provideSeenDebugDrawer(preferences: FlowSharedPreferences): Preference<Boolean> {
        return preferences.getBoolean("debug_seen_debug_drawer", DEFAULT_SEEN_DEBUG_DRAWER)
    }

    @Provides
    @Singleton
    @ScalpelEnabled
    fun provideScalpelEnabled(preferences: FlowSharedPreferences): Preference<Boolean> {
        return preferences.getBoolean("debug_scalpel_enabled", DEFAULT_SCALPEL_ENABLED)
    }

    @Provides
    @Singleton
    @ScalpelWireframeEnabled
    fun provideScalpelWireframeEnabled(preferences: FlowSharedPreferences): Preference<Boolean> {
        return preferences.getBoolean("debug_scalpel_wireframe_drawer",
                DEFAULT_SCALPEL_WIREFRAME_ENABLED)
    }

    companion object {
        private const val DEFAULT_ANIMATION_SPEED = 1 // 1x (normal) speed.
        private const val DEFAULT_PICASSO_DEBUGGING = false // Debug indicators displayed
        private const val DEFAULT_PIXEL_GRID_ENABLED = false // No pixel grid overlay.
        private const val DEFAULT_PIXEL_RATIO_ENABLED = false // No pixel ratio overlay.
        private const val DEFAULT_SCALPEL_ENABLED = false // No crazy 3D view tree.
        private const val DEFAULT_SCALPEL_WIREFRAME_ENABLED = false // Draw views by default.
        private const val DEFAULT_SEEN_DEBUG_DRAWER = false // Show debug drawer first time.
    }
}