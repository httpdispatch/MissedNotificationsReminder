package com.app.missednotificationsreminder.ui.fragment.common;

import com.app.missednotificationsreminder.binding.model.BaseViewModel;

/**
 * Common fragment used as parent for all fragments in the application which has a view model
 *
 * @author Eugene Popovich
 */
public abstract class CommonFragmentWithViewModel<T extends BaseViewModel> extends CommonFragment {

    @Override public void onDestroyView() {
        super.onDestroyView();
        getModel().shutdown();
    }

    /**
     * Get the view model related to the fragment
     *
     * @return
     */
    public abstract T getModel();
}
