package com.example.ticktok.repository;

import androidx.annotation.NonNull;

import java.util.List;

public interface CategoryDataSource {

    final class RawCategoryDocument {
        private final String id;
        private final String title;
        private final String icon;
        private final Object orderValue;

        public RawCategoryDocument(String id, String title, String icon, Object orderValue) {
            this.id = id;
            this.title = title;
            this.icon = icon;
            this.orderValue = orderValue;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getIcon() {
            return icon;
        }

        public Object getOrderValue() {
            return orderValue;
        }
    }

    interface LoadRawCategoriesCallback {
        void onLoaded(List<RawCategoryDocument> documents);
        void onError(Exception exception);
    }

    interface InsertCategoryCallback {
        void onSuccess();
        void onError(Exception exception);
    }

    void getCategories(@NonNull LoadRawCategoriesCallback callback);

    void insertCategoryAtTop(@NonNull String name,
                             @NonNull String icon,
                             @NonNull InsertCategoryCallback callback);
}


