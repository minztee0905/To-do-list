package com.example.ticktok.repository;

import androidx.annotation.NonNull;

import com.example.ticktok.model.Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CategoryRepository implements CategoryRepositoryContract {

    private static final String DEFAULT_ICON = "≡";

    private final CategoryDataSource dataSource;

    public CategoryRepository() {
        this(new FirestoreCategoryDataSource());
    }

    public CategoryRepository(@NonNull CategoryDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void getCategories(@NonNull LoadCategoriesCallback callback) {
        dataSource.getCategories(new CategoryDataSource.LoadRawCategoriesCallback() {
            @Override
            public void onLoaded(List<CategoryDataSource.RawCategoryDocument> documents) {
                    List<Category> categories = new ArrayList<>();

                    for (CategoryDataSource.RawCategoryDocument document : documents) {
                        String id = safeString(document.getId(), "default");
                        String title = safeString(document.getTitle(), "");
                        String icon = safeString(document.getIcon(), DEFAULT_ICON);

                        if (title.isEmpty()) {
                            continue;
                        }

                        int order = parseOrder(document.getOrderValue());
                        categories.add(new Category(id, icon, title, order));
                    }

                    Collections.sort(categories, Comparator.comparingInt(Category::getOrder));
                    callback.onLoaded(categories);
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(exception);
            }
        });
    }

    @Override
    public void addCategory(@NonNull String name,
                            @NonNull String icon,
                            @NonNull OnCategorySavedListener listener) {
        String normalizedName = safeString(name, "");
        if (normalizedName.isEmpty()) {
            listener.onError(new IllegalArgumentException("Tên danh mục không hợp lệ"));
            return;
        }

        String normalizedIcon = safeString(icon, DEFAULT_ICON);
        dataSource.insertCategoryAtTop(normalizedName, normalizedIcon, new CategoryDataSource.InsertCategoryCallback() {
            @Override
            public void onSuccess() {
                listener.onSuccess();
            }

            @Override
            public void onError(Exception exception) {
                listener.onError(exception);
            }
        });
    }

    private int parseOrder(Object orderObj) {
        if (orderObj instanceof Number) {
            return ((Number) orderObj).intValue();
        }
        if (orderObj instanceof String) {
            try {
                return Integer.parseInt((String) orderObj);
            } catch (NumberFormatException ignored) {
            }
        }

        return Integer.MAX_VALUE;
    }

    private String safeString(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}

