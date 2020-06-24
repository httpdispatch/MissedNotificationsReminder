package com.app.missednotificationsreminder.binding.model

import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.app.missednotificationsreminder.data.model.ApplicationItem
import com.app.missednotificationsreminder.data.model.NotificationData
import com.app.missednotificationsreminder.data.model.util.ApplicationIconHandler
import com.app.missednotificationsreminder.di.qualifiers.SelectedApplications
import com.app.missednotificationsreminder.ui.widget.ApplicationsSelectionAdapter
import com.app.missednotificationsreminder.util.asFlow
import com.app.missednotificationsreminder.util.livedata.NonNullMutableLiveData
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
 *
 * @author Eugene Popovich
 */
class ApplicationsSelectionViewModel @Inject constructor(
        @param:SelectedApplications private val selectedApplicationsPref: Preference<Set<String>>,
        private val notificationDataObservable: Observable<List<NotificationData>>,
        private val packageManager: PackageManager) : BaseViewModel() {
    private val _viewState = NonNullMutableLiveData(ViewState(LoadingStatus.NotStarted, Collections.emptyList()))
    val viewState: LiveData<ViewState> = _viewState

    init {
        // initialize data loading
    }

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
        _viewState.value = _viewState.value.copy(loadingStatus = LoadingStatus.Loading)
        notificationDataObservable
                .asFlow()
                .take(1)
                .map { ApplicationsSelectionAdapter.getNotificationCountData(it) }
                .map { notificationsCountInfo ->
                    val result: MutableList<ApplicationItem> = ArrayList()
                    val packages = packageManager.getInstalledPackages(0)
                    val selectedApplications = selectedApplicationsPref.get() ?: emptySet()
                    for (packageInfo in packages) {
                        val selected = selectedApplications.contains(packageInfo.packageName)
                        result.add(ApplicationItem.Builder()
                                .checked(selected)
                                .applicationName(packageInfo.applicationInfo.loadLabel(packageManager))
                                .packageName(packageInfo.packageName)
                                .activeNotifications(if (notificationsCountInfo.containsKey(packageInfo.packageName)) notificationsCountInfo[packageInfo.packageName]!! else 0)
                                .iconUri(Uri.Builder()
                                        .scheme(ApplicationIconHandler.SCHEME)
                                        .authority(packageInfo.packageName)
                                        .build())
                                .build())
                    }
                    result.toList()
                }
                .flowOn(Dispatchers.IO)
                .catch { t ->
                    Timber.e(t, "Unexpected error")
                    _viewState.value = _viewState.value.copy(loadingStatus = LoadingStatus.Error)
                }
                .onEach {
                    _viewState.value = _viewState.value.copy(loadingStatus = LoadingStatus.NotStarted, data = it)
                }
                .launchIn(viewModelScope)
    }
}

data class ViewState(val loadingStatus: LoadingStatus, val data: List<ApplicationItem>)

sealed class LoadingStatus {
    object NotStarted : LoadingStatus()
    object Loading : LoadingStatus()
    object Error : LoadingStatus()
}

