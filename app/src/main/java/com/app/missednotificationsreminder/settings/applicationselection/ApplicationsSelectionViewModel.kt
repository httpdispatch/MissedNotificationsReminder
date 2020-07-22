package com.app.missednotificationsreminder.settings.applicationselection

import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.app.missednotificationsreminder.binding.model.BaseViewStateModel
import com.app.missednotificationsreminder.binding.model.ViewStatePartialChanges
import com.app.missednotificationsreminder.di.qualifiers.SelectedApplications
import com.app.missednotificationsreminder.service.data.model.NotificationData
import com.app.missednotificationsreminder.settings.applicationselection.data.model.util.ApplicationIconHandler
import com.app.missednotificationsreminder.util.asFlow
import com.f2prateek.rx.preferences.Preference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import rx.Observable
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * The view model for the applications selection view
 */
@ExperimentalCoroutinesApi
class ApplicationsSelectionViewModel @Inject constructor(
        @param:SelectedApplications private val selectedApplicationsPref: Preference<Set<String>>,
        private val notificationDataObservable: Observable<List<NotificationData>>,
        private val packageManager: PackageManager) :
        BaseViewStateModel<ViewState, ViewStatePartialChanges<ViewState>>(ViewState(LoadingStatus.NotStarted, Collections.emptyList())) {

    /**
     * Load the application data to the view
     */
    @ExperimentalCoroutinesApi
    fun loadData() {
        Timber.d("loadData: thread=%s", Thread.currentThread().name)
        if (_viewState.value.loadingStatus == LoadingStatus.Loading) {
            Timber.d("loadData: already loading, return")
            return
        }
        _viewState.apply { value = value.copy(loadingStatus = LoadingStatus.Loading) }
        notificationDataObservable
                .asFlow()
                .take(1)
                .map { ApplicationsSelectionAdapter.getNotificationCountData(it) }
                .map { notificationsCountInfo ->
                    val result: MutableList<ApplicationItemViewState> = ArrayList()
                    val packages = packageManager.getInstalledPackages(0)
                    val selectedApplications = selectedApplicationsPref.get() ?: emptySet()
                    for (packageInfo in packages) {
                        val selected = selectedApplications.contains(packageInfo.packageName)
                        result.add(ApplicationItemViewState(
                                checked = selected,
                                applicationName = packageInfo.applicationInfo.loadLabel(packageManager),
                                packageName = packageInfo.packageName,
                                activeNotifications = notificationsCountInfo[packageInfo.packageName]
                                        ?: 0,
                                iconUri = Uri.Builder()
                                        .scheme(ApplicationIconHandler.SCHEME)
                                        .authority(packageInfo.packageName)
                                        .build()))
                    }
                    result.toList()
                }
                .flowOn(Dispatchers.IO)
                .catch { t ->
                    Timber.e(t, "Unexpected error")
                    _viewState.apply { value = value.copy(loadingStatus = LoadingStatus.Error) }
                }
                .onEach {
                    _viewState.apply { value = value.copy(loadingStatus = LoadingStatus.NotStarted, data = it) }
                }
                .launchIn(viewModelScope)
    }
}

data class ViewState(val loadingStatus: LoadingStatus, val data: List<ApplicationItemViewState>)

sealed class LoadingStatus {
    object NotStarted : LoadingStatus()
    object Loading : LoadingStatus()
    object Error : LoadingStatus()
}

