package android.support.v4.app;

import android.util.SparseArray;

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

    public static List<Fragment> fragmentChildFragmentManagerActive(Fragment fragment) {
        return asList(((FragmentManagerImpl) fragment.getChildFragmentManager()).mActive);
    }

    public static <C> List<C> asList(SparseArray<C> sparseArray) {
        if (sparseArray == null) return null;
        List<C> arrayList = new ArrayList<C>(sparseArray.size());
        for (int i = 0; i < sparseArray.size(); i++)
            arrayList.add(sparseArray.valueAt(i));
        return arrayList;
    }
}