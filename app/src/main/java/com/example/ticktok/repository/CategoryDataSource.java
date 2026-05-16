package com.example.ticktok.repository;

import androidx.annotation.NonNull;

import java.util.List;

public interface CategoryDataSource {

    final class CategoryOrderUpdate {
        private final String id;
        private final int order;

        public CategoryOrderUpdate(@NonNull String id, int order) {
            this.id = id;
            this.order = order;
        }

        public String getId() {
            return id;
        }

        public int getOrder() {
            return order;
        }
    }

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

    interface UpdateCategoryCallback {
        void onSuccess();

        void onError(Exception exception);
    }

    interface DeleteCategoryCallback {
        void onSuccess();

        void onError(Exception exception);
    }

    interface UpdateCategoryOrdersCallback {
        void onSuccess();

        void onError(Exception exception);
    }

    void getCategories(@NonNull LoadRawCategoriesCallback callback);

    void insertCategoryAtTop(@NonNull String name,
                             @NonNull String icon,
                             @NonNull InsertCategoryCallback callback);

    void updateCategory(@NonNull String categoryId,
                        @NonNull String name,
                        @NonNull String icon,
                        @NonNull UpdateCategoryCallback callback);

    void deleteCategory(@NonNull String categoryId,
                        @NonNull DeleteCategoryCallback callback);

    void updateCategoryOrders(@NonNull List<CategoryOrderUpdate> updates,
                              @NonNull UpdateCategoryOrdersCallback callback);
}


