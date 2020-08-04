package com.app.missednotificationsreminder.ui.debug

import android.app.Activity
import android.content.Context
import android.os.PowerManager
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.databinding.DebugActivityFrameBinding
import com.app.missednotificationsreminder.ui.AppContainer
import com.jakewharton.u2020.data.*
import com.jakewharton.u2020.ui.bugreport.BugReportLens
import com.jakewharton.u2020.ui.debug.DebugDrawerLayout
import com.jakewharton.u2020.ui.debug.DebugView
import com.mattprecious.telescope.TelescopeLayout
import com.tfcporciuncula.flow.Preference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An [AppContainer] for debug builds which wrap the content view with a sliding drawer on
 * the right that holds all of the debug information and settings.
 */
@Singleton
class DebugAppContainer @Inject constructor(
        private val lumberYard: LumberYard,
        @param:SeenDebugDrawer private val seenDebugDrawer: Preference<Boolean>,
        @param:PixelGridEnabled private val pixelGridEnabled: Preference<Boolean>,
        @param:PixelRatioEnabled private val pixelRatioEnabled: Preference<Boolean>,
        @param:ScalpelEnabled private val scalpelEnabled: Preference<Boolean>,
        @param:ScalpelWireframeEnabled private val scalpelWireframeEnabled: Preference<Boolean>) : AppContainer {
    override fun bind(activity: AppCompatActivity): ViewGroup {
        val binding = DebugActivityFrameBinding.inflate(activity.layoutInflater)
        activity.setContentView(binding.root)
        val drawerContext: Context = ContextThemeWrapper(activity, R.style.Theme_U2020_Debug)
        val debugView = DebugView(drawerContext)
        binding.debugDrawer.addView(debugView)

//    binding.drawerLayout.setDrawerShadow(R.drawable.debug_drawer_shadow, GravityCompat.END);
        binding.debugDrawerLayout.setDrawerListener(object : DebugDrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                debugView.onDrawerOpened()
            }
        })
        TelescopeLayout.cleanUp(activity) // Clean up any old screenshots.
        binding.telescopeContainer.setLens(BugReportLens(activity, lumberYard))

        // If you have not seen the debug drawer before, show it with a message
        if (!seenDebugDrawer.get()) {
            binding.debugDrawerLayout.postDelayed({
                binding.debugDrawerLayout.openDrawer(GravityCompat.END)
                Toast.makeText(drawerContext, R.string.debug_drawer_welcome, Toast.LENGTH_LONG).show()
            }, 1000)
            seenDebugDrawer.set(true)
        }
        setupMadge(binding, activity.lifecycleScope)
        setupScalpel(binding, activity.lifecycleScope)
        riseAndShine(activity)
        return binding.debugContent
    }

    private fun setupMadge(binding: DebugActivityFrameBinding, scope: CoroutineScope) {
        pixelGridEnabled.asFlow()
                .onEach { binding.madgeContainer.isOverlayEnabled = it }
                .launchIn(scope)
        pixelRatioEnabled.asFlow()
                .onEach { binding.madgeContainer.isOverlayRatioEnabled = it }
                .launchIn(scope)
    }

    private fun setupScalpel(binding: DebugActivityFrameBinding, scope: CoroutineScope) {
        scalpelEnabled.asFlow()
                .onEach { binding.debugContent.isLayerInteractionEnabled = it }
                .launchIn(scope)
        scalpelWireframeEnabled.asFlow()
                .onEach { binding.debugContent.setDrawViews(!it) }
                .launchIn(scope)
    }

    companion object {
        /**
         * Show the activity over the lock-screen and wake up the device. If you launched the app manually
         * both of these conditions are already true. If you deployed from the IDE, however, this will
         * save you from hundreds of power button presses and pattern swiping per day!
         */
        fun riseAndShine(activity: Activity) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            val power = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
            val lock = power.newWakeLock(PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE, "mnr:wakeup!")
            lock.acquire()
            lock.release()
        }
    }

}