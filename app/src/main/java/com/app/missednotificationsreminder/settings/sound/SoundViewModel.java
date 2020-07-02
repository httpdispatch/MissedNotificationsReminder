package com.app.missednotificationsreminder.settings.sound;

import android.Manifest;
import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.text.TextUtils;

import com.app.missednotificationsreminder.binding.model.BaseViewModel;
import com.app.missednotificationsreminder.binding.util.BindableString;
import com.app.missednotificationsreminder.di.qualifiers.ForActivity;
import com.app.missednotificationsreminder.di.qualifiers.ForApplication;
import com.app.missednotificationsreminder.di.qualifiers.ReminderRingtone;
import com.f2prateek.rx.preferences.Preference;
import com.tbruyelle.rxpermissions.RxPermissions;

import javax.inject.Inject;

/**
 * The view model for the sound view
 *
 * @author Eugene Popovich
 */
public class SoundViewModel extends BaseViewModel {

    /**
     * Data binding field used to display current ringtone name information
     */
    public BindableString currentRingtoneName = new BindableString();
    /**
     * The activity context
     */
    private Context mContext;
    /**
     * Preference to store/retrieve ringtone URI
     */
    private Preference<String> mRingtone;


    /**
     * @param reminderRingtone preference to store/retrieve reminder interval enabled information
     * @param context          preference the activity context
     */
    @Inject public SoundViewModel(
            @ReminderRingtone Preference<String> reminderRingtone,
            @ForApplication Context context) {
        mRingtone = reminderRingtone;
        mContext = context;
        init();
    }

    void init() {
        monitor(mRingtone.asObservable()
                .filter(__ -> RxPermissions.getInstance(mContext).isGranted(Manifest.permission.READ_EXTERNAL_STORAGE))
                .map(uri -> {
                    if (TextUtils.isEmpty(uri)) {
                        return null;
                    }
                    Ringtone ringtone = RingtoneManager.getRingtone(mContext, Uri.parse(uri));
                    if (ringtone == null) {
                        return null;
                    } else {
                        return ringtone.getTitle(mContext);
                    }
                })
                .subscribe(currentRingtoneName.asAction()));
    }

    /**
     * This method should be called from view when new ringtone is selected
     *
     * @param uri the new ringtone URI
     */
    public void onRingtoneSelected(Uri uri) {
        if (uri == null) {
            mRingtone.set("");
        } else {
            mRingtone.set(uri.toString());
        }
    }

    public String getSelectedRingtone(){
        return mRingtone.get();
    }
}

