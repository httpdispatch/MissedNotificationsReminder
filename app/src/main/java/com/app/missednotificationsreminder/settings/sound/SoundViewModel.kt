package com.app.missednotificationsreminder.settings.sound

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.app.missednotificationsreminder.binding.model.BaseViewStateModel
import com.app.missednotificationsreminder.binding.util.bindWithPreferences
import com.app.missednotificationsreminder.di.qualifiers.ForApplication
import com.app.missednotificationsreminder.settings.di.qualifiers.ReminderRingtone
import com.tfcporciuncula.flow.Preference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * The view model for the sound view
 */
@FlowPreview
@ExperimentalCoroutinesApi
class SoundViewModel @Inject constructor(
        @param:ReminderRingtone private val ringtone: Preference<String>,
        @param:ForApplication private val context: Context) :
        BaseViewStateModel<SoundViewState, SoundViewStatePartialChanges>(SoundViewState(
                ringtone = "",
                ringtoneName = "")) {


    init {
        Timber.d("init")
        viewModelScope.launch {
            launch {
                _viewState.bindWithPreferences(ringtone,
                        { newValue, vs ->
                            vs.copy(ringtone = newValue)
                        },
                        { it.ringtone })
            }
        }
        viewState
                .distinctUntilChanged { previous, next -> previous.ringtone == next.ringtone }
                .onEach { viewState ->
                    if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                            ) == PackageManager.PERMISSION_GRANTED) {
                        val ringtoneName = viewState.ringtone
                                .takeIf { it.isNotEmpty() }
                                ?.let { Uri.parse(it) }
                                ?.let { RingtoneManager.getRingtone(context, it) }
                                ?.getTitle(context)
                                ?: ""
                        process(SoundViewStatePartialChanges.RingtoneTitleChange(ringtoneName))
                    }
                }
                .launchIn(viewModelScope)
    }
}