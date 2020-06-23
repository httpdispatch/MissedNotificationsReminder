package com.app.missednotificationsreminder.binding.util;

import androidx.databinding.BindingAdapter;

import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.app.missednotificationsreminder.R;
import com.appyvet.rangebar.RangeBar;
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxCompoundButton;
import com.jakewharton.rxbinding.widget.RxSeekBar;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.squareup.picasso.RequestCreator;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;
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
    @BindingAdapter({"binding"})
    public static void bindEditText(EditText view,
                                    final BindableString observable) {
        if (view.getTag(R.id.binded) == null) {
            // if the binding was not done before
            view.setTag(R.id.binded, true);
            CompositeSubscription subscription = new CompositeSubscription();
            // subscribe view to the observable value changed event
            subscription.add(RxBindingUtils
                    .valueChanged(observable)
                    .filter(value -> !TextUtils.equals(view.getText(), value)) // filter if value
                    // doesn't need to be updated
                    .subscribe(RxTextView.text(view)))
            ;
            // subscribe observable to the text changes event
            subscription.add(RxTextView.textChanges(view)
                    .map(cs -> cs.toString())
                    .subscribe(observable.asAction()));
            unbindWhenDetached(view, subscription);
        }
    }

    /**
     * Bind the {@link EditText} view with the {@link BindableObject} of the {@link Integer} type
     *
     * @param view       the view to bind observable with
     * @param observable the observable to bind the view with
     */
    @BindingAdapter({"binding"})
    public static void bindEditText(EditText view,
                                    final BindableObject<Integer> observable) {
        if (view.getTag(R.id.binded) == null) {
            // if the binding was not done before
            view.setTag(R.id.binded, true);
            CompositeSubscription subscription = new CompositeSubscription();
            // subscribe view to the observable value changed event
            subscription.add(RxBindingUtils
                    .valueChanged(observable)
                    .map(value -> Integer.toString(value))
                    .filter(value -> !TextUtils.equals(view.getText(), value)) // filter if value
                    // doesn't need to be updated
                    .subscribe(RxTextView.text(view)))
            ;
            // subscribe observable to the text changes event
            Observable<Integer> textChangesObservable = RxTextView.textChanges(view)
                    .debounce(1000, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                    .flatMap(s -> Observable
                            .defer(() -> Observable.just(TextUtils.isEmpty(s) ? null : Integer.parseInt(s.toString())))
                            .onErrorResumeNext(t -> {
                                        Timber.e(t, "onErrorResumeNext");
                                        return Observable.just(null);
                                    }
                            ))
                    .share();
            // if value is not null (no parse error occurred), set it to observable field
            subscription.add(textChangesObservable
                    .filter(v -> v != null)
                    .subscribe(observable.asAction()));
            // if value is null (parse error occurred), set view text to the current observable value and select it so
            // it may be overwritten
            subscription.add(textChangesObservable
                    .filter(v -> v == null)
                    .subscribe(__ -> {
                        view.setText(Integer.toString(observable.get()));
                        view.selectAll();
                    }));
            unbindWhenDetached(view, subscription);
        }
    }

    /**
     * Bind the {@link EditText} view with the {@link BindableObject} of the {@link Float} type
     *
     * @param view       the view to bind observable with
     * @param observable the observable to bind the view with
     */
    @BindingAdapter({"binding"})
    public static void bindEditTextWithFloat(EditText view,
                                             final BindableObject<Float> observable) {
        if (observable == null) {
            // overcome NPE issue at Android 4.3 and below
            return;
        }
        if (view.getTag(R.id.binded) == null) {
            // if the binding was not done before
            view.setTag(R.id.binded, true);
            CompositeSubscription subscription = new CompositeSubscription();
            // subscribe view to the observable value changed event
            subscription.add(RxBindingUtils
                    .valueChanged(observable)
                    .map(value -> Float.toString(value))
                    .filter(value -> !TextUtils.equals(view.getText(), value)) // filter if value
                    // doesn't need to be updated
                    .subscribe(RxTextView.text(view)))
            ;
            // subscribe observable to the text changes event
            Observable<Float> textChangesObservable = RxTextView.textChanges(view)
                    .debounce(1000, TimeUnit.MILLISECONDS)
                    .concatMap(s -> Observable
                            .defer(() -> Observable.just(TextUtils.isEmpty(s) ? null : Float.parseFloat(s.toString())))
                            .onErrorResumeNext(t -> {
                                        Timber.e(t, "onErrorResumeNext");
                                        return Observable.just(null);
                                    }
                            ))
                    .observeOn(AndroidSchedulers.mainThread())
                    .share();
            // if value is not null (no parse error occurred), set it to observable field
            subscription.add(textChangesObservable
                    .filter(v -> v != null)
                    .subscribe(observable.asAction()));
            // if value is null (parse error occurred), set view text to the current observable value and select it so
            // it may be overwritten
            subscription.add(textChangesObservable
                    .filter(v -> v == null)
                    .subscribe(__ -> {
                        view.setText(Float.toString(observable.get()));
                        view.selectAll();
                    }));
            unbindWhenDetached(view, subscription);
        }
    }

    /**
     * Bind the {@link SeekBar} view with the {@link BindableObject} of the {@link Integer} type
     *
     * @param view       the view to bind observable with
     * @param observable the observable to bind the view with
     */
    @BindingAdapter({"binding"})
    public static void bindSeekBar(SeekBar view,
                                   final BindableObject<Integer> observable) {
        if (view.getTag(R.id.binded) == null) {
            // if the binding was not done before
            view.setTag(R.id.binded, true);
            CompositeSubscription subscription = new CompositeSubscription();
            // subscribe view to the observable value changed event
            subscription.add(RxBindingUtils
                    .valueChanged(observable)
                    .filter(value -> value != view.getProgress()) // filter if value
                    // doesn't need to be updated
                    .subscribe(value -> view.setProgress(value)));
            // subscribe observable to the seekbar changes event
            subscription.add(RxSeekBar.changes(view).subscribe(observable.asAction()));
            unbindWhenDetached(view, subscription);
        }
    }

    /**
     * Bind the {@link RangeBar} view with the {@link BindableObject}s of the {@link Integer} type
     *
     * @param view            the view to bind observable with
     * @param leftObservable  the observable to bind the left value of the view with
     * @param rightObservable the observable to bind the right value of the view with
     */
    @BindingAdapter({"bindingLeft", "bindingRight"})
    public static void bindRangeBar(RangeBar view,
                                    final BindableObject<Integer> leftObservable,
                                    final BindableObject<Integer> rightObservable) {
        if (view.getTag(R.id.binded) == null) {
            // if the binding was not done before
            view.setTag(R.id.binded, true);
            CompositeSubscription subscription = new CompositeSubscription();
            // subscribe view to the observable value changed event
            subscription.add(RxBindingUtils
                    .valueChanged(leftObservable)
                    .filter(value -> value != view.getLeftIndex()) // filter if value
                    // doesn't need to be updated
                    .filter(value -> value >= view.getTickStart() / view.getTickInterval())// RangeBar is not stable enough.
                    // Check the value fits the possible range
                    .filter(value -> value <= view.getTickEnd() / view.getTickInterval())// RangeBar is not stable enough.
                    // Check the value fits the possible range
                    .subscribe(value -> view.setRangePinsByIndices(value, rightObservable.get())));
            subscription.add(RxBindingUtils
                    .valueChanged(rightObservable)
                    .filter(value -> value != view.getRightIndex()) // filter if value
                    // doesn't need to be updated
                    .filter(value -> value >= view.getTickStart() / view.getTickInterval())// RangeBar is not stable enough.
                    // Check the value fits the possible range
                    .filter(value -> value <= view.getTickEnd() / view.getTickInterval())// RangeBar is not stable enough.
                    // Check the value fits the possible range
                    .subscribe(value -> view.setRangePinsByIndices(leftObservable.get(), value)));
            // subscribe observable to the rangebar changes event
            view.setOnRangeBarChangeListener((v, left, right, lv, rv) -> {
                int min = (int) (v.getTickStart() / v.getTickInterval());
                int max = (int) (v.getTickEnd() / v.getTickInterval());
                if (left < min) {
                    // if range bar glitch occurs and left index moves outside allowed bounds
                    view.setRangePinsByIndices(min, right);
                } else if (right > max) {
                    // if range bar glitch occurs and right index moves outside allowed bounds
                    view.setRangePinsByIndices(left, max);
                } else {
                    // if values are within the possible range
                    leftObservable.set(left);
                    rightObservable.set(right);
                }
            });
            unbindWhenDetached(view, subscription);
        }
    }

    /**
     * Bind the {@link CompoundButton} view with the {@link BindableBoolean}
     *
     * @param view       the view to bind observable with
     * @param observable the observable to bind the view with
     */
    @BindingAdapter({"binding"})
    public static void bindCompoundButton(CompoundButton view,
                                          final BindableBoolean observable) {
        if (observable == null) {
            // overcome NPE issue at Android 4.3 and below
            return;
        }
        if (view.getTag(R.id.binded) == null) {
            // if the binding was not done before
            view.setTag(R.id.binded, true);
            CompositeSubscription subscription = new CompositeSubscription();
            // subscribe view to the observable value changed event
            subscription.add(RxBindingUtils
                    .valueChanged(observable)
                    .filter(value -> value != view.isChecked()) // filter if value
                    // doesn't need to be updated
                    .subscribe(RxCompoundButton.checked(view)))
            ;
            // subscribe observable to the compound button checked changes event
            subscription.add(RxCompoundButton.checkedChanges(view).subscribe(observable.asAction()));
            unbindWhenDetached(view, subscription);
        }
    }

    private static void unbindWhenDetached(View view, CompositeSubscription subscription) {
        subscription.add(RxView.detaches(view)
                .subscribe(__ -> {
                    view.setTag(R.id.binded, null);
                    subscription.clear();
                }));
    }

    /**
     * Bind the {@link ImageView} view with the {@link RequestCreator}
     *
     * @param view           the view to bind request creator with
     * @param requestCreator the request creator to bind the view with
     */
    @BindingAdapter({"request"})
    public static void loadImage(ImageView view, RequestCreator requestCreator) {
        if (requestCreator == null) {
            view.setImageBitmap(null);
        } else {
            // load
            try {
                // load
                requestCreator.into(view);
            } catch (Exception e) {
                // catch unexpected IllegalArgumentException errors
                Timber.e(e);
            }
        }
    }
}
