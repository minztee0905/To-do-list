package com.example.ticktok.repository;

import androidx.annotation.NonNull;

import com.example.ticktok.model.Category;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CategoryRepository {

    private static final String COLLECTION_DEFAULT_CATEGORIES = "default_categories";

    private final FirebaseFirestore firestore;

    public interface LoadCategoriesCallback {
        void onLoaded(List<Category> categories);
        void onError(Exception exception);
    }

    public CategoryRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void getCategories(@NonNull LoadCategoriesCallback callback) {
        firestore.collection(COLLECTION_DEFAULT_CATEGORIES)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Category> categories = new ArrayList<>();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String id = safeString(document.getId(), "default");
                        String title = safeString(document.getString("title"), "");
                        String icon = safeString(document.getString("icon"), "✨");

                        if (title.isEmpty()) {
                            continue;
                        }

                        int order = parseOrder(document);
                        categories.add(new Category(id, icon, title, order));
                    }

                    Collections.sort(categories, Comparator.comparingInt(Category::getOrder));
                    callback.onLoaded(categories);
                })
                .addOnFailureListener(callback::onError);
    }

    private int parseOrder(QueryDocumentSnapshot document) {
        String orderText = safeString(document.getString("order"), "");
        if (!orderText.isEmpty()) {
            try {
                return Integer.parseInt(orderText);
            } catch (NumberFormatException ignored) {
                // Fall through to numeric fallback.
            }
        }

        Long orderLong = document.getLong("order");
        if (orderLong != null) {
            return orderLong.intValue();
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

