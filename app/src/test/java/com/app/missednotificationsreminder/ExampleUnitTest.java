package com.app.missednotificationsreminder;

import com.app.missednotificationsreminder.binding.util.RxBindingUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test public void shareProblem() throws InterruptedException {
        BindableObject<Integer> begin = new BindableObject<>(0);
        BindableObject<Integer> rangeBegin = new BindableObject<>(0);
        BindableObject<Integer> mSchedulerRangeBegin = new BindableObject<>(0);
        BindableObject<String> beginTime = new BindableObject<>(null);

        begin.set(50);
        rangeBegin.set(10);
        mSchedulerRangeBegin.set(50);

        Observable<Integer> beginChanged = valueChanged(begin)
                .doOnEach(value -> System.out.println("Begin value changed: " + value.getValue()))
                .doOnUnsubscribe(() -> System.out.println("Unshared?!"))
                .share();

        beginChanged
                .skip(1)// skip initial value emitted automatically right after the
                        // subsription
                .debounce(500, TimeUnit.MILLISECONDS)// such as range bar may change the
                        // value very quickly use the
                        // debounce function for the timeout
                        // based processing
                .doOnEach(value -> System.out.println("Begin value changed 2: " + value.getValue()))
                .subscribe(mSchedulerRangeBegin.asAction(), t -> t.printStackTrace());
        beginChanged
                .map(minutes -> Integer.toString(minutes))
                .subscribe(beginTime.asAction(), t -> t.printStackTrace());
        beginChanged
                .map(minutes -> minutes / 5)
                .subscribe(rangeBegin.asAction(), t -> t.printStackTrace());


        valueChanged(rangeBegin)
                .map(v -> v * 5)
                .doOnEach(value -> Timber.d("Range begin value changed: " + value.getValue()))
                .filter(minutes -> Math.abs(minutes - begin.get()) > 5)
                .doOnEach(value -> Timber.d("Range begin filtered value changed: " + value.getValue()))
                .subscribe(begin.asAction());

        begin.set(100);
        Thread.sleep(1000);

        assertEquals(beginTime.get(), "100");
        assertTrue(rangeBegin.get() == 20);

        Observable.just(200)
                .subscribeOn(Schedulers.newThread())
                .subscribe(begin.asAction());
        begin.set(200);
        Thread.sleep(1000);

        assertEquals(beginTime.get(), "200");
        assertTrue(rangeBegin.get() == 40);

        assertTrue(mSchedulerRangeBegin.get() == 200);

        begin.set(300);
        Thread.sleep(1000);

        assertEquals(beginTime.get(), "300");
        assertTrue(rangeBegin.get() == 60);

        assertTrue(mSchedulerRangeBegin.get() == 300);

    }

    public static <T> Observable<T> valueChanged(BindableObject<T> object) {
        return Observable.create(new BindableObjectValueChangedOnSubscribe<T>(object));
    }

    public static class BindableObjectValueChangedOnSubscribe<T> implements Observable.OnSubscribe<T> {
        private final BindableObject<T> mObject;

        /**
         * Creates an BindableObjectValueChangedOnSubscribe for the given bindableObject
         *
         * @param bindableObject the bindable object to create
         */
        public BindableObjectValueChangedOnSubscribe(BindableObject<T> bindableObject) {
            this.mObject = bindableObject;
        }

        @Override public void call(final Subscriber<? super T> subscriber) {
            // create the property changed callback for the BindableObject which emits property value
            // to the subscriber when it is changed
            Observable2.OnPropertyChangedCallback callback = new Observable2.OnPropertyChangedCallback() {
                @Override public void onPropertyChanged(Observable2 sender, int propertyId) {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(mObject.get());
                    }
                }
            };
            mObject.addOnPropertyChangedCallback(callback);

            // when the subscription become unsubscribed remove added above property changed callback
            subscriber.add(
                    Subscriptions.create(
                            () -> mObject.removeOnPropertyChangedCallback(callback)));

            // Emit initial value.
            subscriber.onNext(mObject.get());
        }
    }

    public class BindableObject<T> extends BaseObservable {
        T mValue;

        /**
         * Creates an empty observable object
         */
        public BindableObject() {
        }

        /**
         * Wraps the given object and creates an observable object
         *
         * @param value The value to be wrapped as an observable.
         */
        public BindableObject(T value) {
            mValue = value;
        }

        /**
         * Set the stored value.
         */
        public void set(T value) {
            if (!equals(mValue, value)) {
                mValue = value;
                notifyChange();
            }
        }

        public T get() {
            return mValue;
        }

        /**
         * Compare two objects of the same type. Supports null comparison as well as {@linkplain
         * Object#equals(Object) Object.equals()} method.
         * <p>
         * Override this method if you need additional comparison logic, for example custom string
         * comparison.
         *
         * @param o1
         * @param o2
         * @return true if objects are equal, false otherwise
         */
        public boolean equals(T o1, T o2) {
            return (o1 == o2) || (o1 != null && o1.equals(o2));
        }

        /**
         * An action which stores a new value for this object.
         */
        public Action1<? super T> asAction() {
            return new Action1<T>() {
                @Override public void call(T value) {
                    set(value);
                }
            };
        }
    }

    public interface Observable2 {

        /**
         * Adds a callback to listen for changes to the Observable.
         *
         * @param callback The callback to start listening.
         */
        void addOnPropertyChangedCallback(OnPropertyChangedCallback callback);

        /**
         * Removes a callback from those listening for changes.
         *
         * @param callback The callback that should stop listening.
         */
        void removeOnPropertyChangedCallback(OnPropertyChangedCallback callback);

        /**
         * The callback that is called by Observable when an observable property has changed.
         */
        abstract class OnPropertyChangedCallback {

            /**
             * Called by an Observable whenever an observable property changes.
             *
             * @param sender     The Observable that is changing.
             * @param propertyId The BR identifier of the property that has changed. The getter
             *                   for this property should be annotated with {Bindable}.
             */
            public abstract void onPropertyChanged(Observable2 sender, int propertyId);
        }
    }

    public class BaseObservable implements Observable2 {
        private transient PropertyChangeRegistry mCallbacks;

        public BaseObservable() {
        }

        @Override
        public synchronized void addOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
            if (mCallbacks == null) {
                mCallbacks = new PropertyChangeRegistry();
            }
            mCallbacks.add(callback);
        }

        @Override
        public synchronized void removeOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
            if (mCallbacks != null) {
                mCallbacks.remove(callback);
            }
        }

        /**
         * Notifies listeners that all properties of this instance have changed.
         */
        public synchronized void notifyChange() {
            if (mCallbacks != null) {
                mCallbacks.notifyCallbacks(this, 0, null);
            }
        }

        /**
         * Notifies listeners that a specific property has changed. The getter for the property
         * that changes should be marked with Bindable to generate a field in
         * <code>BR</code> to be used as <code>fieldId</code>.
         *
         * @param fieldId The generated BR id for the Bindable field.
         */
        public void notifyPropertyChanged(int fieldId) {
            if (mCallbacks != null) {
                mCallbacks.notifyCallbacks(this, fieldId, null);
            }
        }
    }

    public static class PropertyChangeRegistry extends
            CallbackRegistry<Observable2.OnPropertyChangedCallback, Observable2, Void> {

        private static final CallbackRegistry.NotifierCallback<Observable2.OnPropertyChangedCallback, Observable2, Void> NOTIFIER_CALLBACK = new CallbackRegistry.NotifierCallback<Observable2.OnPropertyChangedCallback, Observable2, Void>() {
            @Override
            public void onNotifyCallback(Observable2.OnPropertyChangedCallback callback, Observable2 sender,
                                         int arg, Void notUsed) {
                callback.onPropertyChanged(sender, arg);
            }
        };

        public PropertyChangeRegistry() {
            super(NOTIFIER_CALLBACK);
        }

        /**
         * Notifies registered callbacks that a specific property has changed.
         *
         * @param observable The Observable that has changed.
         * @param propertyId The BR id of the property that has changed or BR._all if the entire
         *                   Observable has changed.
         */
        public void notifyChange(Observable2 observable, int propertyId) {
            notifyCallbacks(observable, propertyId, null);
        }
    }

    public static class CallbackRegistry<C, T, A> implements Cloneable {
        private static final String TAG = "CallbackRegistry";

        /**
         * An ordered collection of listeners waiting to be notified.
         */
        private List<C> mCallbacks = new ArrayList<C>();

        /**
         * A bit flag for the first 64 listeners that are removed during notification.
         * The lowest significant bit corresponds to the 0th index into mCallbacks.
         * For a small number of callbacks, no additional array of objects needs to
         * be allocated.
         */
        private long mFirst64Removed = 0x0;

        /**
         * Bit flags for the remaining callbacks that are removed during notification.
         * When there are more than 64 callbacks and one is marked for removal, a dynamic
         * array of bits are allocated for the callbacks.
         */
        private long[] mRemainderRemoved;

        /**
         * The recursion level of the notification
         */
        private int mNotificationLevel;

        /**
         * The notification mechanism for notifying an event.
         */
        private final NotifierCallback<C, T, A> mNotifier;

        /**
         * Creates an EventRegistry that notifies the event with notifier.
         *
         * @param notifier The class to use to notify events.
         */
        public CallbackRegistry(NotifierCallback<C, T, A> notifier) {
            mNotifier = notifier;
        }

        /**
         * Notify all callbacks.
         *
         * @param sender The originator. This is an opaque parameter passed to
         *               {@link CallbackRegistry.NotifierCallback#onNotifyCallback(Object, Object, int, Object)}
         * @param arg    An opaque parameter passed to
         *               {@link CallbackRegistry.NotifierCallback#onNotifyCallback(Object, Object, int, Object)}
         * @param arg2   An opaque parameter passed to
         *               {@link CallbackRegistry.NotifierCallback#onNotifyCallback(Object, Object, int, Object)}
         */
        public synchronized void notifyCallbacks(T sender, int arg, A arg2) {
            mNotificationLevel++;
            notifyRecurse(sender, arg, arg2);
            mNotificationLevel--;
            if (mNotificationLevel == 0) {
                if (mRemainderRemoved != null) {
                    for (int i = mRemainderRemoved.length - 1; i >= 0; i--) {
                        final long removedBits = mRemainderRemoved[i];
                        if (removedBits != 0) {
                            removeRemovedCallbacks((i + 1) * Long.SIZE, removedBits);
                            mRemainderRemoved[i] = 0;
                        }
                    }
                }
                if (mFirst64Removed != 0) {
                    removeRemovedCallbacks(0, mFirst64Removed);
                    mFirst64Removed = 0;
                }
            }
        }

        /**
         * Notify up to the first Long.SIZE callbacks that don't have a bit set in <code>removed</code>.
         *
         * @param sender The originator. This is an opaque parameter passed to
         *               {@link CallbackRegistry.NotifierCallback#onNotifyCallback(Object, Object, int, Object)}
         * @param arg    An opaque parameter passed to
         *               {@link CallbackRegistry.NotifierCallback#onNotifyCallback(Object, Object, int, Object)}
         * @param arg2   An opaque parameter passed to
         *               {@link CallbackRegistry.NotifierCallback#onNotifyCallback(Object, Object, int, Object)}
         */
        private void notifyFirst64(T sender, int arg, A arg2) {
            final int maxNotified = Math.min(Long.SIZE, mCallbacks.size());
            notifyCallbacks(sender, arg, arg2, 0, maxNotified, mFirst64Removed);
        }

        /**
         * Notify all callbacks using a recursive algorithm to avoid allocating on the heap.
         * This part captures the callbacks beyond Long.SIZE that have no bits allocated for
         * removal before it recurses into {@link #notifyRemainder(Object, int, A, int)}.
         * <p>
         * <p>Recursion is used to avoid allocating temporary state on the heap.</p>
         *
         * @param sender The originator. This is an opaque parameter passed to
         *               {@link CallbackRegistry.NotifierCallback#onNotifyCallback(Object, Object, int, Object)}
         * @param arg    An opaque parameter passed to
         *               {@link CallbackRegistry.NotifierCallback#onNotifyCallback(Object, Object, int, Object)}
         * @param arg2   An opaque parameter passed to
         *               {@link CallbackRegistry.NotifierCallback#onNotifyCallback(Object, Object, int, Object)}
         */
        private void notifyRecurse(T sender, int arg, A arg2) {
            final int callbackCount = mCallbacks.size();
            final int remainderIndex = mRemainderRemoved == null ? -1 : mRemainderRemoved.length - 1;

            // Now we've got all callbakcs that have no mRemainderRemoved value, so notify the
            // others.
            notifyRemainder(sender, arg, arg2, remainderIndex);

            // notifyRemainder notifies all at maxIndex, so we'd normally start at maxIndex + 1
            // However, we must also keep track of those in mFirst64Removed, so we add 2 instead:
            final int startCallbackIndex = (remainderIndex + 2) * Long.SIZE;

            // The remaining have no bit set
            notifyCallbacks(sender, arg, arg2, startCallbackIndex, callbackCount, 0);
        }

        /**
         * Notify callbacks that have mRemainderRemoved bits set for remainderIndex. If
         * remainderIndex is -1, the first 64 will be notified instead.
         *
         * @param sender         The originator. This is an opaque parameter passed to
         *                       {@link CallbackRegistry.NotifierCallback#onNotifyCallback(Object, Object, int, Object)}
         * @param arg            An opaque parameter passed to
         *                       {@link CallbackRegistry.NotifierCallback#onNotifyCallback(Object, Object, int, Object)}
         * @param arg2           An opaque parameter passed to
         *                       {@link CallbackRegistry.NotifierCallback#onNotifyCallback(Object, Object, int, Object)}
         * @param remainderIndex The index into mRemainderRemoved that should be notified.
         */
        private void notifyRemainder(T sender, int arg, A arg2, int remainderIndex) {
            if (remainderIndex < 0) {
                notifyFirst64(sender, arg, arg2);
            } else {
                final long bits = mRemainderRemoved[remainderIndex];
                final int startIndex = (remainderIndex + 1) * Long.SIZE;
                final int endIndex = Math.min(mCallbacks.size(), startIndex + Long.SIZE);
                notifyRemainder(sender, arg, arg2, remainderIndex - 1);
                notifyCallbacks(sender, arg, arg2, startIndex, endIndex, bits);
            }
        }

        /**
         * Notify callbacks from startIndex to endIndex, using bits as the bit status
         * for whether they have been removed or not. bits should be from mRemainderRemoved or
         * mFirst64Removed. bits set to 0 indicates that all callbacks from startIndex to
         * endIndex should be notified.
         *
         * @param sender     The originator. This is an opaque parameter passed to
         *                   {@link CallbackRegistry.NotifierCallback#onNotifyCallback(Object, Object, int, Object)}
         * @param arg        An opaque parameter passed to
         *                   {@link CallbackRegistry.NotifierCallback#onNotifyCallback(Object, Object, int, Object)}
         * @param arg2       An opaque parameter passed to
         *                   {@link CallbackRegistry.NotifierCallback#onNotifyCallback(Object, Object, int, Object)}
         * @param startIndex The index into the mCallbacks to start notifying.
         * @param endIndex   One past the last index into mCallbacks to notify.
         * @param bits       A bit field indicating which callbacks have been removed and shouldn't
         *                   be notified.
         */
        private void notifyCallbacks(T sender, int arg, A arg2, final int startIndex,
                                     final int endIndex, final long bits) {
            long bitMask = 1;
            for (int i = startIndex; i < endIndex; i++) {
                if ((bits & bitMask) == 0) {
                    mNotifier.onNotifyCallback(mCallbacks.get(i), sender, arg, arg2);
                }
                bitMask <<= 1;
            }
        }

        /**
         * Add a callback to be notified. If the callback is already in the list, another won't
         * be added. This does not affect current notifications.
         *
         * @param callback The callback to add.
         */
        public synchronized void add(C callback) {
            int index = mCallbacks.lastIndexOf(callback);
            if (index < 0 || isRemoved(index)) {
                mCallbacks.add(callback);
            }
        }

        /**
         * Returns true if the callback at index has been marked for removal.
         *
         * @param index The index into mCallbacks to check.
         * @return true if the callback at index has been marked for removal.
         */
        private boolean isRemoved(int index) {
            if (index < Long.SIZE) {
                // It is in the first 64 callbacks, just check the bit.
                final long bitMask = 1L << index;
                return (mFirst64Removed & bitMask) != 0;
            } else if (mRemainderRemoved == null) {
                // It is after the first 64 callbacks, but nothing else was marked for removal.
                return false;
            } else {
                final int maskIndex = (index / Long.SIZE) - 1;
                if (maskIndex >= mRemainderRemoved.length) {
                    // There are some items in mRemainderRemoved, but nothing at the given index.
                    return false;
                } else {
                    // There is something marked for removal, so we have to check the bit.
                    final long bits = mRemainderRemoved[maskIndex];
                    final long bitMask = 1L << (index % Long.SIZE);
                    return (bits & bitMask) != 0;
                }
            }
        }

        /**
         * Removes callbacks from startIndex to startIndex + Long.SIZE, based
         * on the bits set in removed.
         *
         * @param startIndex The index into the mCallbacks to start removing callbacks.
         * @param removed    The bits indicating removal, where each bit is set for one callback
         *                   to be removed.
         */
        private void removeRemovedCallbacks(int startIndex, long removed) {
            // The naive approach should be fine. There may be a better bit-twiddling approach.
            final int endIndex = startIndex + Long.SIZE;

            long bitMask = 1L << (Long.SIZE - 1);
            for (int i = endIndex - 1; i >= startIndex; i--) {
                if ((removed & bitMask) != 0) {
                    mCallbacks.remove(i);
                }
                bitMask >>>= 1;
            }
        }

        /**
         * Remove a callback. This callback won't be notified after this call completes.
         *
         * @param callback The callback to remove.
         */
        public synchronized void remove(C callback) {
            if (mNotificationLevel == 0) {
                mCallbacks.remove(callback);
            } else {
                int index = mCallbacks.lastIndexOf(callback);
                if (index >= 0) {
                    setRemovalBit(index);
                }
            }
        }

        private void setRemovalBit(int index) {
            if (index < Long.SIZE) {
                // It is in the first 64 callbacks, just check the bit.
                final long bitMask = 1L << index;
                mFirst64Removed |= bitMask;
            } else {
                final int remainderIndex = (index / Long.SIZE) - 1;
                if (mRemainderRemoved == null) {
                    mRemainderRemoved = new long[mCallbacks.size() / Long.SIZE];
                } else if (mRemainderRemoved.length < remainderIndex) {
                    // need to make it bigger
                    long[] newRemainders = new long[mCallbacks.size() / Long.SIZE];
                    System.arraycopy(mRemainderRemoved, 0, newRemainders, 0, mRemainderRemoved.length);
                    mRemainderRemoved = newRemainders;
                }
                final long bitMask = 1L << (index % Long.SIZE);
                mRemainderRemoved[remainderIndex] |= bitMask;
            }
        }

        /**
         * Makes a copy of the registered callbacks and returns it.
         *
         * @return a copy of the registered callbacks.
         */
        public synchronized ArrayList<C> copyCallbacks() {
            ArrayList<C> callbacks = new ArrayList<C>(mCallbacks.size());
            int numListeners = mCallbacks.size();
            for (int i = 0; i < numListeners; i++) {
                if (!isRemoved(i)) {
                    callbacks.add(mCallbacks.get(i));
                }
            }
            return callbacks;
        }

        /**
         * Modifies <code>callbacks</code> to contain all callbacks in the CallbackRegistry.
         *
         * @param callbacks modified to contain all callbacks registered to receive events.
         */
        public synchronized void copyCallbacks(List<C> callbacks) {
            callbacks.clear();
            int numListeners = mCallbacks.size();
            for (int i = 0; i < numListeners; i++) {
                if (!isRemoved(i)) {
                    callbacks.add(mCallbacks.get(i));
                }
            }
        }

        /**
         * Returns true if there are no registered callbacks or false otherwise.
         *
         * @return true if there are no registered callbacks or false otherwise.
         */
        public synchronized boolean isEmpty() {
            if (mCallbacks.isEmpty()) {
                return true;
            } else if (mNotificationLevel == 0) {
                return false;
            } else {
                int numListeners = mCallbacks.size();
                for (int i = 0; i < numListeners; i++) {
                    if (!isRemoved(i)) {
                        return false;
                    }
                }
                return true;
            }
        }

        /**
         * Removes all callbacks from the list.
         */
        public synchronized void clear() {
            if (mNotificationLevel == 0) {
                mCallbacks.clear();
            } else if (!mCallbacks.isEmpty()) {
                for (int i = mCallbacks.size() - 1; i >= 0; i--) {
                    setRemovalBit(i);
                }
            }
        }

        /**
         * @return A copy of the CallbackRegistry with all callbacks listening to both instances.
         */
        @SuppressWarnings("unchecked")
        public synchronized CallbackRegistry<C, T, A> clone() {
            CallbackRegistry<C, T, A> clone = null;
            try {
                clone = (CallbackRegistry<C, T, A>) super.clone();
                clone.mFirst64Removed = 0;
                clone.mRemainderRemoved = null;
                clone.mNotificationLevel = 0;
                clone.mCallbacks = new ArrayList<C>();
                final int numListeners = mCallbacks.size();
                for (int i = 0; i < numListeners; i++) {
                    if (!isRemoved(i)) {
                        clone.mCallbacks.add(mCallbacks.get(i));
                    }
                }
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
            return clone;
        }

        /**
         * Class used to notify events from CallbackRegistry.
         *
         * @param <C> The callback type.
         * @param <T> The notification sender type. Typically this is the containing class.
         * @param <A> An opaque argument to pass to the notifier
         */
        public abstract static class NotifierCallback<C, T, A> {
            /**
             * Called by CallbackRegistry during
             * {@link CallbackRegistry#notifyCallbacks(Object, int, Object)}} to notify the callback.
             *
             * @param callback The callback to notify.
             * @param sender   The opaque sender object.
             * @param arg      The opaque notification parameter.
             * @param arg2     An opaque argument passed in
             *                 {@link CallbackRegistry#notifyCallbacks}
             * @see CallbackRegistry#CallbackRegistry(CallbackRegistry.NotifierCallback)
             */
            public abstract void onNotifyCallback(C callback, T sender, int arg, A arg2);
        }
    }

}