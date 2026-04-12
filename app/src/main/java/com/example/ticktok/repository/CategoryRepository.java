package com.example.ticktok.repository;

import androidx.annotation.NonNull;

import com.example.ticktok.model.Category;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryRepository {

    private static final String COLLECTION_CATEGORIES = "categories";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_ICON = "icon";
    private static final String FIELD_ORDER = "order";
    private static final String DEFAULT_ICON = "≡";

    private final FirebaseFirestore db;
    private final CollectionReference categoriesRef;

    public interface LoadCategoriesCallback {
        void onLoaded(List<Category> categories);
        void onError(Exception exception);
    }

    public interface OnCategorySavedListener {
        void onSuccess(String message);

        void onError(Exception e);
    }

    public CategoryRepository() {
        db = FirebaseFirestore.getInstance();
        categoriesRef = db.collection(COLLECTION_CATEGORIES);
    }

    public void getCategories(@NonNull LoadCategoriesCallback callback) {
        categoriesRef
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Category> categories = new ArrayList<>();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String id = safeString(document.getId(), "default");
                        String title = safeString(document.getString(FIELD_TITLE), "");
                        String icon = safeString(document.getString(FIELD_ICON), DEFAULT_ICON);

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

    public void addCategory(@NonNull String name,
                            @NonNull String icon,
                            @NonNull OnCategorySavedListener listener) {
        String normalizedName = safeString(name, "");
        if (normalizedName.isEmpty()) {
            listener.onError(new IllegalArgumentException("Tên danh mục không hợp lệ"));
            return;
        }

        String normalizedIcon = safeString(icon, DEFAULT_ICON);

        categoriesRef
                .get()
                .addOnSuccessListener(snapshot -> runInsertAndShiftBatch(snapshot, normalizedName, normalizedIcon, listener))
                .addOnFailureListener(listener::onError);
    }

    private void runInsertAndShiftBatch(@NonNull QuerySnapshot snapshot,
                                        @NonNull String name,
                                        @NonNull String icon,
                                        @NonNull OnCategorySavedListener listener) {
        WriteBatch batch = db.batch();

        for (QueryDocumentSnapshot document : snapshot) {
            DocumentReference docRef = document.getReference();
            int currentOrder = parseOrder(document);
            int nextOrder = currentOrder == Integer.MAX_VALUE ? Integer.MAX_VALUE : currentOrder + 1;
            batch.update(docRef, FIELD_ORDER, nextOrder);
        }

        DocumentReference newDocRef = categoriesRef.document();
        Map<String, Object> newCategory = new HashMap<>();

        newCategory.put(FIELD_TITLE, name);
        newCategory.put(FIELD_ICON, icon);
        newCategory.put(FIELD_ORDER, 1);
        batch.set(newDocRef, newCategory);

        batch.commit()
                .addOnSuccessListener(unused -> listener.onSuccess("Thêm danh mục thành công"))
                .addOnFailureListener(listener::onError);
    }

    private int parseOrder(QueryDocumentSnapshot document) {
        Object orderObj = document.get(FIELD_ORDER);

        if (orderObj instanceof Number) {
            return ((Number) orderObj).intValue();
        }
        else if (orderObj instanceof String) {
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

