package com.app.missednotificationsreminder.ui.widget;

import android.content.pm.PackageManager;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.app.missednotificationsreminder.binding.model.ApplicationItemViewModel;
import com.app.missednotificationsreminder.data.model.ApplicationItem;
import com.app.missednotificationsreminder.databinding.ApplicationSelectableItemViewBinding;
import com.app.missednotificationsreminder.ui.fragment.ApplicationsSelectionFragment;
import com.squareup.picasso.Picasso;

import java.util.List;

import javax.inject.Inject;

import rx.functions.Action1;
import timber.log.Timber;

/**
 * {@link RecyclerView} adapter to provide applications selection functionality in the
 * {@linkplain ApplicationsSelectionFragment applications selection view}
 *
 * @author Eugene Popovich
 */
public final class ApplicationsSelectionAdapter extends RecyclerView.Adapter<ApplicationsSelectionAdapter.ViewHolder>
        implements Action1<List<ApplicationItem>> {

    private final ApplicationItemViewModel.ApplicationCheckedStateChangedListener mCheckedStateChangedListener;

    private PackageManager mPackageManager;

    private Picasso mPicasso;

    private SortedList<ApplicationItem> mData =  new SortedList<ApplicationItem>(ApplicationItem.class, new SortedListAdapterCallback<ApplicationItem>(this) {
        @Override
        public int compare(ApplicationItem t0, ApplicationItem t1) {
            if (t0.isChecked() != t1.isChecked()) {
                return t0.isChecked() ? -1 : 1;
            }
            return getLabel(t0).compareToIgnoreCase(getLabel(t1));
        }

        String getLabel(ApplicationItem item){
            CharSequence result = item.getPpackageInfo().applicationInfo.loadLabel(mPackageManager);
            if(result == null){
                result = "";
            }
            return result.toString();
        }

        @Override
        public boolean areContentsTheSame(ApplicationItem oldItem,
                                          ApplicationItem newItem) {
            return getLabel(oldItem).equals(getLabel(newItem));
        }

        @Override
        public boolean areItemsTheSame(ApplicationItem item1, ApplicationItem item2) {
            return item1.getPpackageInfo() == item2.getPpackageInfo();
        }
    });

    /**
     * @param applicationCheckedStateChangedListener the listener for the application checked/unchecked event
     * @param packageManager
     * @param picasso
     */
    @Inject public ApplicationsSelectionAdapter(
            ApplicationItemViewModel.ApplicationCheckedStateChangedListener applicationCheckedStateChangedListener,
            PackageManager packageManager, Picasso picasso) {
        this.mCheckedStateChangedListener = applicationCheckedStateChangedListener;
        mPackageManager = packageManager;
        mPicasso = picasso;
        setHasStableIds(false);
    }


    @Override public void call(List<ApplicationItem> data) {
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
                    new ApplicationItemViewModel(item.isChecked(), item.getPpackageInfo(), mPackageManager,
                            mPicasso,
                            (packageInfo, checked) -> {
                                Timber.d("Update checked value to %1$b", checked);
                                item.setChecked(checked);
                                // change item position in view
                                int adapterPosition = getAdapterPosition();
                                if (adapterPosition != RecyclerView.NO_POSITION) {
                                    mData.recalculatePositionOfItemAt(adapterPosition);
                                }
                                // notify global listener if exists
                                if (mCheckedStateChangedListener != null) {
                                    mCheckedStateChangedListener.onApplicationCheckedStateChanged(packageInfo, checked);
                                }
                            }));
        }
    }
}
