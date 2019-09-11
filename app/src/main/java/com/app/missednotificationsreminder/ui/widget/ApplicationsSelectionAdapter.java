package com.app.missednotificationsreminder.ui.widget;

import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.app.missednotificationsreminder.binding.model.ApplicationItemViewModel;
import com.app.missednotificationsreminder.data.model.ApplicationItem;
import com.app.missednotificationsreminder.data.model.NotificationData;
import com.app.missednotificationsreminder.databinding.ApplicationSelectableItemViewBinding;
import com.app.missednotificationsreminder.ui.fragment.ApplicationsSelectionFragment;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import androidx.core.util.ObjectsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;
import androidx.recyclerview.widget.SortedListAdapterCallback;
import rx.Completable;
import rx.Emitter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * {@link RecyclerView} adapter to provide applications selection functionality in the
 * {@linkplain ApplicationsSelectionFragment applications selection view}
 *
 * @author Eugene Popovich
 */
public final class ApplicationsSelectionAdapter extends RecyclerView.Adapter<ApplicationsSelectionAdapter.ViewHolder> {

    private final ApplicationItemViewModel.ApplicationCheckedStateChangedListener mCheckedStateChangedListener;

    private PackageManager mPackageManager;

    private Picasso mPicasso;

    private CompositeSubscription mSubscription = new CompositeSubscription();

    private SortedList<ApplicationItem> mData = new SortedList<>(ApplicationItem.class, new SortedListAdapterCallback<ApplicationItem>(this) {
        @Override
        public int compare(ApplicationItem t0, ApplicationItem t1) {
            if (t0.activeNotifications != t1.activeNotifications) {
                return t1.activeNotifications - t0.activeNotifications;
            }
            if (t0.checked != t1.checked) {
                return t0.checked ? -1 : 1;
            }
            return getLabel(t0).compareToIgnoreCase(getLabel(t1));
        }

        String getLabel(ApplicationItem item) {
            CharSequence result = item.applicationName;
            if (result == null) {
                result = "";
            }
            return result.toString();
        }

        @Override
        public boolean areContentsTheSame(ApplicationItem oldItem,
                                          ApplicationItem newItem) {
            return ObjectsCompat.equals(oldItem, newItem);
        }

        @Override
        public boolean areItemsTheSame(ApplicationItem item1, ApplicationItem item2) {
            return item1 == item2;
        }
    });

    /**
     * @param applicationCheckedStateChangedListener the listener for the application checked/unchecked event
     * @param packageManager
     * @param picasso
     */
    @Inject public ApplicationsSelectionAdapter(
            ApplicationItemViewModel.ApplicationCheckedStateChangedListener applicationCheckedStateChangedListener,
            Observable<List<NotificationData>> notificationDataObservable,
            PackageManager packageManager, Picasso picasso) {
        this.mCheckedStateChangedListener = applicationCheckedStateChangedListener;
        mPackageManager = packageManager;
        mPicasso = picasso;
        setHasStableIds(false);
        mSubscription.add(notificationDataObservable
                .onBackpressureLatest()
                .observeOn(AndroidSchedulers.mainThread())
                .map(ApplicationsSelectionAdapter::getNotificationCountData)
                .flatMapCompletable(notificationsCountInfo -> Completable.fromAction(() -> {
                    while (true) {
                        boolean found = false;
                        for (int i = 0; i < mData.size(); i++) {
                            ApplicationItem item = mData.get(i);
                            Integer count = notificationsCountInfo.get(item.packageName);
                            if (count == null) {
                                count = 0;
                            }
                            if (item.activeNotifications != count.intValue()) {
                                mData.updateItemAt(i, new ApplicationItem.Builder(item).activeNotifications(count).build());
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            break;
                        }
                    }
                }))
                .subscribe());
    }

    @NotNull public static Map<String, Integer> getNotificationCountData(List<NotificationData> notificationData) {
        Map<String, Integer> result = new HashMap<>();
        for (NotificationData notification : notificationData) {
            Integer count = result.get(notification.packageName);
            if (count == null) {
                count = 0;
            }
            count = count + 1;
            result.put(notification.packageName, count);
        }
        return result;
    }


    public void setData(List<ApplicationItem> data) {
        mData.clear();
        mData.addAll(data);
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        ApplicationSelectableItemViewBinding binding = ApplicationSelectableItemViewBinding.inflate(inflater, viewGroup, false);
        return new ViewHolder(binding);
    }

    @Override public void onBindViewHolder(ViewHolder viewHolder, int i) {
        viewHolder.bindTo(mData.get(i));
    }

    @Override public long getItemId(int position) {
        return mData.get(position).hashCode();
    }

    @Override public int getItemCount() {
        return mData.size();
    }

    public void shutdown() {
        mSubscription.clear();
    }

    /**
     * View holder implementation for this adapter
     */
    public final class ViewHolder extends RecyclerView.ViewHolder {
        ApplicationSelectableItemViewBinding binding;

        public ViewHolder(ApplicationSelectableItemViewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bindTo(final ApplicationItem item) {
            binding.setModel(
                    new ApplicationItemViewModel(item,
                            mPicasso,
                            (packageInfo, checked) -> {
                                Timber.d("Update checked value to %1$b", checked);
                                mData.updateItemAt(getAdapterPosition(), new ApplicationItem.Builder(item).checked(checked).build());
                                // notify global listener if exists
                                if (mCheckedStateChangedListener != null) {
                                    mCheckedStateChangedListener.onApplicationCheckedStateChanged(packageInfo, checked);
                                }
                            }));
        }
    }
}
