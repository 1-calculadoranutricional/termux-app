package com.termux.app.store;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.termux.R;

import java.io.File;
import java.util.List;

public class StorePackageAdapter extends RecyclerView.Adapter<StorePackageAdapter.PackageViewHolder> {

    public interface OnPackageActionListener {
        void onActionClick(StorePackageItem item, boolean isInstalled);
        void onSecondaryActionClick(StorePackageItem item);
    }

    private final List<StorePackageItem> mItems;
    private final OnPackageActionListener mListener;

    public StorePackageAdapter(List<StorePackageItem> items, OnPackageActionListener listener) {
        this.mItems = items;
        this.mListener = listener;
    }

    public void updateList(List<StorePackageItem> newItems) {
        mItems.clear();
        mItems.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PackageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_store_package, parent, false);
        return new PackageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PackageViewHolder holder, int position) {
        StorePackageItem item = mItems.get(position);

        holder.titleView.setText(item.getTitle());
        holder.categoryView.setText(item.getCategory() + " • " + item.getRating() + " • " + item.getSize());
        holder.descriptionView.setText(item.getDescription());
        holder.iconView.setImageResource(item.getIconResId());

        if (item.getIconResId() == R.drawable.ic_store_yazi || item.getIconResId() == R.drawable.ic_store_antigravity) {
            holder.iconView.setPadding(0, 0, 0, 0);
            holder.iconView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            int pad = (int) (8 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
            holder.iconView.setPadding(pad, pad, pad, pad);
            holder.iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }

        boolean isInstalled = false;
        if (item.getBinaryCheckPath() != null) {
            isInstalled = new File(item.getBinaryCheckPath()).exists();
        }

        if (item.getBinaryCheckPath() == null) {
            // Task/utility action (e.g. Storage, Backup, Restore)
            holder.actionButton.setText("Ejecutar");
            holder.secondaryButton.setVisibility(View.GONE);
        } else if (isInstalled) {
            holder.actionButton.setText("Actualizar");
            if (item.getUninstallCommand() != null) {
                holder.secondaryButton.setVisibility(View.VISIBLE);
                holder.secondaryButton.setText("Desinstalar");
                holder.secondaryButton.setOnClickListener(v -> {
                    if (mListener != null) mListener.onSecondaryActionClick(item);
                });
            } else {
                holder.secondaryButton.setVisibility(View.GONE);
            }
        } else {
            holder.actionButton.setText("Instalar");
            holder.secondaryButton.setVisibility(View.GONE);
        }

        final boolean finalIsInstalled = isInstalled;
        holder.actionButton.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onActionClick(item, finalIsInstalled);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    static class PackageViewHolder extends RecyclerView.ViewHolder {
        final ImageView iconView;
        final TextView titleView;
        final TextView categoryView;
        final TextView descriptionView;
        final MaterialButton actionButton;
        final MaterialButton secondaryButton;

        PackageViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.package_icon);
            titleView = itemView.findViewById(R.id.package_title);
            categoryView = itemView.findViewById(R.id.package_category);
            descriptionView = itemView.findViewById(R.id.package_description);
            actionButton = itemView.findViewById(R.id.package_action_button);
            secondaryButton = itemView.findViewById(R.id.package_secondary_button);
        }
    }
}
