package com.app.missednotificationsreminder.settings.sound;

import android.app.Activity;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.app.missednotificationsreminder.R;
import com.app.missednotificationsreminder.databinding.FragmentSoundBinding;
import com.app.missednotificationsreminder.ui.fragment.common.CommonFragmentWithViewModel;

import javax.inject.Inject;

import dagger.android.ContributesAndroidInjector;

/**
 * Fragment which displays sound settings view
 *
 * @author Eugene Popovich
 */
public class SoundFragment extends CommonFragmentWithViewModel<SoundViewModel> {
    /**
     * The request code used to run ringtone picker
     */
    static final int SELECT_RINGTONE_REQUEST_CODE = 0;

    @Inject SoundViewModel model;
    FragmentSoundBinding mBinding;

    @Override public SoundViewModel getModel() {
        return model;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mBinding = FragmentSoundBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view, savedInstanceState);
    }

    private void init(View view, Bundle savedInstanceState) {
        mBinding.setFragment(this);
        mBinding.setModel(model);
    }

    /**
     * Method which is called when select ringtone button is clicked. It launches the system
     * ringtone picker window.
     *
     * @param v
     */
    public void onSoundButtonClicked(View v) {
        selectRingtone(model.getSelectedRingtone());
    }

    public void selectRingtone(String currentRingtoneUri) {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.sound_select_ringtone_dialog_title));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                TextUtils.isEmpty(currentRingtoneUri) ? null : Uri.parse(currentRingtoneUri));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        startActivityForResult(intent, SELECT_RINGTONE_REQUEST_CODE);
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == SELECT_RINGTONE_REQUEST_CODE) {
            // if received successful response from the ringtone picker
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            model.onRingtoneSelected(uri);
        }
    }

    @dagger.Module
    public static abstract class Module {
        @ContributesAndroidInjector
        abstract SoundFragment contribute();
    }
}
