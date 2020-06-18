package com.app.missednotificationsreminder.ui.activity

import com.app.missednotificationsreminder.binding.model.ApplicationItemViewModel.ApplicationCheckedStateChangedListener
import com.app.missednotificationsreminder.data.model.ApplicationItem
import com.app.missednotificationsreminder.di.qualifiers.ActivityScope
import com.app.missednotificationsreminder.di.qualifiers.SelectedApplications
import com.app.missednotificationsreminder.ui.fragment.ApplicationsSelectionFragment
import com.app.missednotificationsreminder.ui.view.ApplicationsSelectionView
import com.f2prateek.rx.preferences.Preference
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import timber.log.Timber

/**
 * The Dagger dependency injection module for the applications selection activity
 */
@Module
abstract class ApplicationsSelectionActivityModule {
    @ActivityScope
    @ContributesAndroidInjector(
            modules = [
                ApplicationsSelectionActivityModuleExt::class,
                ApplicationsSelectionFragment.Module::class
            ]
    )
    abstract fun contributeSettingsActivity(): ApplicationsSelectionActivity
}

@Module
class ApplicationsSelectionActivityModuleExt {
    @Provides
    fun provideApplicationsSelectionView(activity: ApplicationsSelectionActivity): ApplicationsSelectionView {
        return activity.applicationsSelectionFragment
    }

    @Provides @ActivityScope
    fun provideApplicationsCheckedStateChangeListener(@SelectedApplications selectedApplications: Preference<Set<String>>): ApplicationCheckedStateChangedListener {
        return ApplicationCheckedStateChangedListener { applicationItem: ApplicationItem, checked: Boolean ->
            Timber.d("Update selected application value %1\$s to %2\$b", applicationItem.packageName, checked)
            // for sure we may use if condition here instead of concatenation of 2 observables. Just wanted to achieve
            // same result with RxJava usage.
            (selectedApplications.get() ?: emptySet())
                    .let {
                        val updatedSet = it.toMutableSet();
                        if (updatedSet.contains(applicationItem.packageName))
                            updatedSet.remove(applicationItem.packageName)
                        else
                            updatedSet.add(applicationItem.packageName)
                        selectedApplications.set(updatedSet.toSet());
                    }
        }
    }

}