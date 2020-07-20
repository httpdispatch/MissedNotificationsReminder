package com.app.missednotificationsreminder.settings.sound

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.databinding.FragmentSoundBinding
import com.app.missednotificationsreminder.di.ViewModelKey
import com.app.missednotificationsreminder.ui.fragment.common.CommonFragmentWithViewBinding
import dagger.Binds
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import timber.log.Timber
import javax.inject.Inject

/**
 * Fragment which displays sound settings view
 */
@FlowPreview
@ExperimentalCoroutinesApi
class SoundFragment : CommonFragmentWithViewBinding<FragmentSoundBinding>(
        R.layout.fragment_sound) {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by viewModels<SoundViewModel> { viewModelFactory }

    private val selectRingtone = registerForActivityResult(PickRingtoneContract()) { uri ->
        Timber.d("Obtained result: $uri")
        if (uri != null) {
            viewModel.process(SoundViewStatePartialChanges.RingtoneChange(uri))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        // Set the lifecycle owner to the lifecycle of the view
        viewDataBinding.apply {
            lifecycleOwner = viewLifecycleOwner
            fragment = this@SoundFragment
            viewState = viewModel.viewState.asLiveData()
        }
    }


    /**
     * Method which is called when select ringtone button is clicked. It launches the system
     * ringtone picker window.
     *
     * @param v
     */
    fun onSoundButtonClicked(v: View?) {
        selectRingtone.launch(viewModel.viewState.value.ringtone)
    }

    class PickRingtoneContract : ActivityResultContract<String?, Uri?>() {

        override fun createIntent(context: Context, input: String?): Intent {
            return Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(R.string.sound_select_ringtone_dialog_title))
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                        input?.let { Uri.parse(it) })
                putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return when (resultCode) {
                Activity.RESULT_OK -> intent?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                        ?: Uri.EMPTY
                else -> null
            }
        }
    }

    @dagger.Module
    abstract class Module {
        @ContributesAndroidInjector
        abstract fun contribute(): SoundFragment?

        @Binds
        @IntoMap
        @ViewModelKey(SoundViewModel::class)
        internal abstract fun bindViewModel(viewModel: SoundViewModel): ViewModel
    }
}