package android.support.v4.app;

import java.util.ArrayList;
import java.util.List;

/**
 * Taken from https://github.com/shamanland/nested-fragment-issue to fix issue with nested fragment
 * doesn't receive onActivityResult
 */
public class SupportV4App {
    public static void activityFragmentsNoteStateNotSaved(FragmentActivity activity) {
        activity.mFragments.noteStateNotSaved();
    }

    public static List<Fragment> activityFragmentsActive(FragmentActivity activity) {
        return activity.mFragments.getActiveFragments(null);
    }

    public static int fragmentIndex(Fragment fragment) {
        return fragment.mIndex;
    }

    public static ArrayList<Fragment> fragmentChildFragmentManagerActive(Fragment fragment) {
        return ((FragmentManagerImpl) fragment.getChildFragmentManager()).mActive;
    }
}