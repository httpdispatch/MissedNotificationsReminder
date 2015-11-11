package com.app.missednotificationsreminder.ui.view;

import com.app.missednotificationsreminder.data.model.ApplicationItem;

import java.util.List;

import rx.functions.Action1;

/**
 * Applications selection view interface
 *
 * @author Eugene Popovich
 */
public interface ApplicationsSelectionView {

    /**
     * Switch the view to the loading state
     */
    void setLoadingState();

    /**
     * Switch the view to the error state
     */
    void setErrorState();

    /**
     * Get the Rx action to handle loaded application data
     *
     * @return
     */
    Action1<List<ApplicationItem>> getListLoadedAction();
}
