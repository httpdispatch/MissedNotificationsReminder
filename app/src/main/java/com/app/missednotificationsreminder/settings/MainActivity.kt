package com.app.missednotificationsreminder.settings

import android.content.Context
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.databinding.ActivityMainBinding
import com.app.missednotificationsreminder.di.qualifiers.ActivityScope
import com.app.missednotificationsreminder.di.qualifiers.ForActivity
import com.app.missednotificationsreminder.ui.activity.common.CommonFragmentActivity
import com.app.missednotificationsreminder.settings.applicationselection.ApplicationsSelectionFragment
import dagger.Provides
import dagger.android.ContributesAndroidInjector

class MainActivity : CommonFragmentActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navController: NavController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration.Builder(R.id.settingsFragment)
                .build()
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host_fragment).navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    /**
     * The Dagger dependency injection module for the settings activity
     */
    @dagger.Module()
    abstract class Module {
        @ActivityScope
        @ContributesAndroidInjector(
                modules = [
                    ModuleExt::class,
                    SettingsFragment.Module::class,
                    ApplicationsSelectionFragment.Module::class
                ]
        )
        abstract fun contribute(): MainActivity
    }


    @dagger.Module
    class ModuleExt {
        /**
         * Allow the activity context to be injected but require that it be annotated with
         * [@ForActivity][ForActivity] to explicitly differentiate it from an application context.
         */
        @Provides
        @ForActivity
        fun provideActivityContext(activity: MainActivity): Context {
            return activity
        }
    }
}
