package com.app.missednotificationsreminder.binding.util;

import android.databinding.BindingAdapter;
import android.text.TextUtils;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.app.missednotificationsreminder.R;
import com.jakewharton.rxbinding.widget.RxCompoundButton;
import com.jakewharton.rxbinding.widget.RxSeekBar;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.squareup.picasso.RequestCreator;

import rx.Observable;
import timber.log.Timber;

/**
 * Utility class which stores Android Data Binding framework related tag binding rules
 *
 * @author Eugene Popovich
 */
public class BindingAdapterUtils {
    /**
     * Bind the {@link EditText} view with the {@link BindableString}
     *
     * @param view       the view to bind observable with
     * @param observable the observable to bind the view with
     */
    @BindingAdapter({"bind:binding"})
    public static void bindEditText(EditText view,
                                    final BindableString observable) {
        if (view.getTag(R.id.dataBinding) == null) {
            // if the binding was not done before
            view.setTag(R.id.dataBinding, true);
            // subscribe view to the observable value changed event
            RxBindingUtils
                    .valueChanged(observable)
                    .filter(value -> !TextUtils.equals(view.getText(), value)) // filter if value
                            // doesn't need to be updated
                    .subscribe(RxTextView.text(view))
            ;
            // subscribe observable to the text changes event
            RxTextView.textChanges(view)
                    .map(cs -> cs.toString())
                    .subscribe(observable.asAction());
        }
    }

    /**
     * Bind the {@link EditText} view with the {@link BindableObject} of the {@link Integer} type
     *
     * @param view       the view to bind observable with
     * @param observable the observable to bind the view with
     */
    @BindingAdapter({"bind:binding"})
    public static void bindEditText(EditText view,
                                    final BindableObject<Integer> observable) {
        if (view.getTag(R.id.dataBinding) == null) {
            // if the binding was not done before
            view.setTag(R.id.dataBinding, true);
            // subscribe view to the observable value changed event
            RxBindingUtils
                    .valueChanged(observable)
                    .map(value -> Integer.toString(value))
                    .filter(value -> !TextUtils.equals(view.getText(), value)) // filter if value
                            // doesn't need to be updated
                    .subscribe(RxTextView.text(view))
            ;
            // subscribe observable to the text changes event
            RxTextView.textChanges(view)
                    .flatMap(s -> Observable
                            .defer(() -> Observable.just(TextUtils.isEmpty(s) ? 0 : Integer.parseInt(s.toString())))
                            .onErrorResumeNext(t -> {
                                        Timber.e(t, "onErrorResumeNext");
                                        return Observable.just(0, observable.get());
                                    }
                            ))
                    .subscribe(observable.asAction());
        }
    }

    /**
     * Bind the {@link SeekBar} view with the {@link BindableObject} of the {@link Integer} type
     *
     * @param view       the view to bind observable with
     * @param observable the observable to bind the view with
     */
    @BindingAdapter({"bind:binding"})
    public static void bindSeekBar(SeekBar view,
                                   final BindableObject<Integer> observable) {
        if (view.getTag(R.id.dataBinding) == null) {
            // if the binding was not done before
            view.setTag(R.id.dataBinding, true);
            // subscribe view to the observable value changed event
            RxBindingUtils
                    .valueChanged(observable)
                    .filter(value -> value != view.getProgress()) // filter if value
                            // doesn't need to be updated
                    .subscribe(value -> view.setProgress(value));
            // subscribe observable to the seekbar changes event
            RxSeekBar.changes(view).subscribe(observable.asAction());
        }
    }

    /**
     * Bind the {@link CompoundButton} view with the {@link BindableBoolean}
     *
     * @param view       the view to bind observable with
     * @param observable the observable to bind the view with
     */
    @BindingAdapter({"bind:binding"})
    public static void bindCompoundButton(CompoundButton view,
                                          final BindableBoolean observable) {
        if (view.getTag(R.id.dataBinding) == null) {
            // if the binding was not done before
            view.setTag(R.id.dataBinding, true);
            // subscribe view to the observable value changed event
            RxBindingUtils
                    .valueChanged(observable)
                    .filter(value -> value != view.isChecked()) // filter if value
                            // doesn't need to be updated
                    .subscribe(RxCompoundButton.checked(view))
            ;
            // subscribe observable to the compound button checked changes event
            RxCompoundButton.checkedChanges(view).subscribe(observable.asAction());
        }
    }

    /**
     * Bind the {@link ImageView} view with the {@link RequestCreator}
     *
     * @param view           the view to bind request creator with
     * @param requestCreator the request creator to bind the view with
     */
    @BindingAdapter({"bind:request"})
    public static void loadImage(ImageView view, RequestCreator requestCreator) {
        if (requestCreator == null) {
            view.setImageBitmap(null);
        } else {
            // load
            requestCreator.into(view);
        }
    }
}
