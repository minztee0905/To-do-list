package com.example.ticktok.repository;

import androidx.annotation.NonNull;

import com.example.ticktok.model.Category;

import java.util.List;

public interface CategoryRepositoryContract {

    interface LoadCategoriesCallback {
        void onLoaded(List<Category> categories);
        void onError(Exception exception);
    }

    interface OnCategorySavedListener {
        void onSuccess();
        void onError(Exception e);
    }

    void getCategories(@NonNull LoadCategoriesCallback callback);

    void addCategory(@NonNull String name,
                     @NonNull String icon,
                     @NonNull OnCategorySavedListener listener);
}


