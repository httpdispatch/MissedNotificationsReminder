package com.app.missednotificationsreminder.binding.model;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;

import com.app.missednotificationsreminder.binding.util.BindableString;
import com.app.missednotificationsreminder.di.qualifiers.ForActivity;
import com.app.missednotificationsreminder.di.qualifiers.ReminderRingtone;
import com.app.missednotificationsreminder.ui.view.SoundView;
import com.f2prateek.rx.preferences.Preference;

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
     * The related view.
     */
    private SoundView mView;
    /**
     * The activity context
     */
    private Context mContext;
    /**
     * Preference to store/retrieve ringtone URI
     */
    private Preference<String> mRingtone;


    /**
     * @param view             the related view
     * @param reminderRingtone preference to store/retrieve reminder interval enabled information
     * @param context          preference the activity context
     */
    @Inject public SoundViewModel(
            SoundView view,
            @ReminderRingtone Preference<String> reminderRingtone,
            @ForActivity Context context) {
        mView = view;
        mRingtone = reminderRingtone;
        mContext = context;
        init();
    }

    void init() {
        monitor(mRingtone.asObservable()
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

    /**
     * Method which is called when select ringtone button is clicked. It launches the system
     * ringtone picker window.
     *
     * @param v
     */
    public void onSoundButtonClicked(View v) {
        mView.selectRingtone(mRingtone.get());
    }

}

