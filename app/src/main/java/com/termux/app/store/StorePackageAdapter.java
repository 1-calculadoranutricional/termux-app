package com.termux.app.store;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
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
        void onItemClick(StorePackageItem item);
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
        String ratingText = item.getRating() + " • " + item.getSize();
        SpannableString ratingSpannable = new SpannableString(ratingText);
        int starIdx = ratingText.indexOf('★');
        if (starIdx >= 0) {
            ratingSpannable.setSpan(new ForegroundColorSpan(Color.parseColor("#FFFFFF")),
                starIdx, starIdx + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        holder.ratingSizeView.setText(ratingSpannable);
        holder.descriptionView.setText(item.getDescription());
        holder.iconView.setImageResource(item.getIconResId());

        int pad = (int) (8 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
        holder.iconView.setPadding(pad, pad, pad, pad);
        holder.iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        boolean isInstalled = false;
        if (item.getBinaryCheckPath() != null) {
            for (String path : item.getBinaryCheckPath().split(":")) {
                if (new File(path).exists()) {
                    isInstalled = true;
                    break;
                }
            }
        }

        if (item.getBinaryCheckPath() == null) {
            // Task/utility action (e.g. Storage, Backup, Restore, Mirrors)
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

        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemClick(item);
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
        final TextView ratingSizeView;
        final TextView descriptionView;
        final MaterialButton actionButton;
        final MaterialButton secondaryButton;

        PackageViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.package_icon);
            titleView = itemView.findViewById(R.id.package_title);
            ratingSizeView = itemView.findViewById(R.id.package_rating_size);
            descriptionView = itemView.findViewById(R.id.package_description);
            actionButton = itemView.findViewById(R.id.package_action_button);
            secondaryButton = itemView.findViewById(R.id.package_secondary_button);
        }
    }
}
