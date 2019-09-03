package com.app.missednotificationsreminder.ui.activity;

import android.util.Pair;

import com.app.missednotificationsreminder.ApplicationModule;
import com.app.missednotificationsreminder.binding.model.ApplicationItemViewModel;
import com.app.missednotificationsreminder.binding.model.ApplicationsSelectionViewModel;
import com.app.missednotificationsreminder.di.qualifiers.SelectedApplications;
import com.app.missednotificationsreminder.ui.fragment.ApplicationsSelectionFragment;
import com.app.missednotificationsreminder.ui.view.ApplicationsSelectionView;
import com.app.missednotificationsreminder.ui.widget.ApplicationsSelectionAdapter;
import com.f2prateek.rx.preferences.Preference;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import rx.Observable;
import timber.log.Timber;

/**
 * The Dagger dependency injection module for the applications selection activity
 */
@Module(
        addsTo = ApplicationModule.class,
        injects = {
                ApplicationsSelectionFragment.class,
                ApplicationsSelectionViewModel.class,
                ApplicationsSelectionAdapter.class,
        }
)
public final class ApplicationsSelectionActivityModule {
    private final ApplicationsSelectionActivity mApplicationsSelectionActivity;

    ApplicationsSelectionActivityModule(ApplicationsSelectionActivity applicationsSelectionActivity) {
        this.mApplicationsSelectionActivity = applicationsSelectionActivity;
    }

    @Provides
    @Singleton ApplicationsSelectionView provideApplicationsSelectionView() {
        return mApplicationsSelectionActivity.getApplicationsSelectionFragment();
    }

    @Provides
    @Singleton ApplicationItemViewModel.ApplicationCheckedStateChangedListener provideApplicationsCheckedStateChangeListener(@SelectedApplications Preference<Set<String>> selectedApplications) {
        return (applicationItem, checked) -> {
            Timber.d("Update selected application value %1$s to %2$b", applicationItem.packageName, checked);
            Observable<Pair<String, Set<String>>> selection = Observable
                    .just(new Pair<>(applicationItem.packageName, selectedApplications.get()))
                    .share();
            // for sure we may use if condition here instead of concatenation of 2 observables. Just wanted to achieve
            // same result with RxJava usage.
            selection
                    .filter(pair -> pair.second.contains(pair.first))
                    .map(pair -> {
                        Timber.d("Removing application from ApplicationsSelection");
                        Set<String> result = new HashSet<String>(pair.second);
                        result.remove(pair.first);
                        return result;
                    })
                    .concatWith(
                            selection.filter(pair -> !pair.second.contains(pair.first))
                                    .map(pair -> {
                                        Timber.d("Adding application to ApplicationsSelection");
                                        Set<String> result = new HashSet<String>(pair.second);
                                        result.add(pair.first);
                                        return result;
                                    })
                    )
                    .subscribe(selectedApplications.asAction());
        };
    }
}