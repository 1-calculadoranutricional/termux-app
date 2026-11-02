package com.termux.app.store;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class StorePackageItem {

    public enum ActionType {
        INSTALL,
        UPDATE,
        UNINSTALL,
        EXECUTE
    }

    private final String id;
    private final String title;
    private final String categoryId;
    private final String category;
    private final String description;
    @DrawableRes
    private final int iconResId;
    private final String installCommand;
    @Nullable
    private final String uninstallCommand;
    @Nullable
    private final String updateCommand;
    @Nullable
    private final String binaryCheckPath;
    private final String rating;
    private final String size;
    @Nullable
    private final String initGuide;
    @Nullable
    private final String helpGuide;
    @Nullable
    private final String quickTip;

    public StorePackageItem(@NonNull String id,
                            @NonNull String title,
                            @NonNull String categoryId,
                            @NonNull String category,
                            @NonNull String description,
                            @DrawableRes int iconResId,
                            @NonNull String installCommand,
                            @Nullable String uninstallCommand,
                            @Nullable String updateCommand,
                            @Nullable String binaryCheckPath) {
        this(id, title, categoryId, category, description, iconResId, installCommand, uninstallCommand, updateCommand, binaryCheckPath, "4.8 ★", "12 MB", null, null, null);
    }

    public StorePackageItem(@NonNull String id,
                            @NonNull String title,
                            @NonNull String categoryId,
                            @NonNull String category,
                            @NonNull String description,
                            @DrawableRes int iconResId,
                            @NonNull String installCommand,
                            @Nullable String uninstallCommand,
                            @Nullable String updateCommand,
                            @Nullable String binaryCheckPath,
                            @NonNull String rating,
                            @NonNull String size) {
        this(id, title, categoryId, category, description, iconResId, installCommand, uninstallCommand, updateCommand, binaryCheckPath, rating, size, null, null, null);
    }

    public StorePackageItem(@NonNull String id,
                            @NonNull String title,
                            @NonNull String categoryId,
                            @NonNull String category,
                            @NonNull String description,
                            @DrawableRes int iconResId,
                            @NonNull String installCommand,
                            @Nullable String uninstallCommand,
                            @Nullable String updateCommand,
                            @Nullable String binaryCheckPath,
                            @NonNull String rating,
                            @NonNull String size,
                            @Nullable String initGuide,
                            @Nullable String helpGuide,
                            @Nullable String quickTip) {
        this.id = id;
        this.title = title;
        this.categoryId = categoryId;
        this.category = category;
        this.description = description;
        this.iconResId = iconResId;
        this.installCommand = installCommand;
        this.uninstallCommand = uninstallCommand;
        this.updateCommand = updateCommand;
        this.binaryCheckPath = binaryCheckPath;
        this.rating = rating;
        this.size = size;
        this.initGuide = initGuide;
        this.helpGuide = helpGuide;
        this.quickTip = quickTip;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getInstallCommand() {
        return installCommand;
    }

    @Nullable
    public String getUninstallCommand() {
        return uninstallCommand;
    }

    @Nullable
    public String getUpdateCommand() {
        return updateCommand;
    }

    @Nullable
    public String getBinaryCheckPath() {
        return binaryCheckPath;
    }

    public String getRating() {
        return rating;
    }

    public String getSize() {
        return size;
    }

    @Nullable
    public String getInitGuide() {
        return initGuide;
    }

    @Nullable
    public String getHelpGuide() {
        return helpGuide;
    }

    @Nullable
    public String getQuickTip() {
        return quickTip;
    }
}
