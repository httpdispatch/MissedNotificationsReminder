package com.app.missednotificationsreminder.util.event;

import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

/**
 * Implementation of the event bus based on the RsJava technology.
 * courtesy: https://gist.github.com/benjchristensen/04eef9ca0851f3a5d7bf
 */
public class RxEventBus {

    // If multiple threads are going to emit events to this
    // then it must be made thread-safe like this instead
    private final Subject<Event, Event> _bus = new SerializedSubject<>(PublishSubject.create());

    /**
     * Send the event to all the subscribers
     *
     * @param event the event to send
     */
    public void send(Event event) {
        _bus.onNext(event);
    }

    /**
     * Get the observable from the current event bus object to subscribe new observers
     *
     * @return
     */
    public Observable<Event> toObserverable() {
        return _bus;
    }

    /**
     * Whether the event bus has any observers subscribers
     *
     * @return
     */
    public boolean hasObservers() {
        return _bus.hasObservers();
    }
}
