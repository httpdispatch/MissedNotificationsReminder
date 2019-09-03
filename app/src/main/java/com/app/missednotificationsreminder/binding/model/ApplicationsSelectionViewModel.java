package com.app.missednotificationsreminder.binding.model;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.app.missednotificationsreminder.data.model.ApplicationItem;
import com.app.missednotificationsreminder.data.model.NotificationData;
import com.app.missednotificationsreminder.data.model.util.ApplicationIconHandler;
import com.app.missednotificationsreminder.di.qualifiers.IoThreadScheduler;
import com.app.missednotificationsreminder.di.qualifiers.MainThreadScheduler;
import com.app.missednotificationsreminder.di.qualifiers.SelectedApplications;
import com.app.missednotificationsreminder.ui.view.ApplicationsSelectionView;
import com.app.missednotificationsreminder.ui.widget.ApplicationsSelectionAdapter;
import com.f2prateek.rx.preferences.Preference;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import rx.Observable;
import rx.Scheduler;
import rx.Single;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import timber.log.Timber;

/**
 * The view model for the applications selection view
 *
 * @author Eugene Popovich
 */
public class ApplicationsSelectionViewModel extends BaseViewModel {
    private ApplicationsSelectionView mView;
    private Preference<Set<String>> mSelectedApplications;
    private Scheduler mMainThreadScheduler;
    private Scheduler mIoThreadScheduler;
    private PackageManager mPackageManager;
    Observable<List<NotificationData>> mNotificationDataObservable;

    private PublishSubject<Set<String>> dataLoadSubject;

    /**
     * @param view                 the related view
     * @param selectedApplications preference to store/retrieve selected applications
     * @param mainThreadScheduler
     * @param ioThreadScheduler
     * @param packageManager
     */
    @Inject public ApplicationsSelectionViewModel(ApplicationsSelectionView view, @SelectedApplications Preference<Set<String>> selectedApplications,
                                                  Observable<List<NotificationData>> notificationDataObservable,
                                                  @MainThreadScheduler Scheduler mainThreadScheduler,
                                                  @IoThreadScheduler Scheduler ioThreadScheduler,
                                                  PackageManager packageManager) {
        this.mMainThreadScheduler = mainThreadScheduler;
        this.mIoThreadScheduler = ioThreadScheduler;
        this.mView = view;
        this.mPackageManager = packageManager;
        this.mSelectedApplications = selectedApplications;
        mNotificationDataObservable = notificationDataObservable;
        init();
    }

    private void init() {

        // initialize data loading
        dataLoadSubject = PublishSubject.create();

        Observable<List<ApplicationItem>> result = dataLoadSubject //
                .flatMapSingle(this::packagesList) // load data
                .observeOn(mMainThreadScheduler) //
                .share();
        monitor(result //
                .subscribe(mView.getListLoadedAction(), errorHandler));
    }

    private Single<List<ApplicationItem>> packagesList(Set<String> selectedPreferences) {
        return mNotificationDataObservable
                .take(1)
                .toSingle()
                .map(ApplicationsSelectionAdapter::getNotificationCountData)
                .flatMap(notificationsCountInfo -> Single.fromCallable(() -> {
                    List<ApplicationItem> result = new ArrayList<>();
                    List<PackageInfo> packages = mPackageManager.getInstalledPackages(0);
                    for (PackageInfo packageInfo : packages) {
                        boolean selected = selectedPreferences.contains(packageInfo.packageName);
                        result.add(new ApplicationItem.Builder()
                                .checked(selected)
                                .applicationName(packageInfo.applicationInfo.loadLabel(mPackageManager))
                                .packageName(packageInfo.packageName)
                                .activeNotifications(notificationsCountInfo.containsKey(packageInfo.packageName) ? notificationsCountInfo.get(packageInfo.packageName) : 0)
                                .iconUri(new Uri.Builder()
                                        .scheme(ApplicationIconHandler.SCHEME)
                                        .authority(packageInfo.packageName)
                                        .build())
                                .build());
                    }
                    return result;
                }).subscribeOn(mIoThreadScheduler));
    }

    /**
     * Load the application data to the view
     */
    public void loadData() {
        mView.setLoadingState();
        dataLoadSubject.onNext(mSelectedApplications.get());
    }


    /**
     * The action to handle error which may occur during data loading operation
     */
    private final Action1<Throwable> errorHandler = t -> {
        Timber.e(t, "Unexpected error");
        mView.setErrorState();
    };
}
