package com.example.ticktok.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.example.ticktok.util.UserFirestorePaths;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreCategoryDataSource implements CategoryDataSource {

    private static final String COLLECTION_CATEGORIES = "categories";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_ICON = "icon";
    private static final String FIELD_ORDER = "order";

    private final FirebaseFirestore db;

    public FirestoreCategoryDataSource() {
        this(FirebaseFirestore.getInstance());
    }

    public FirestoreCategoryDataSource(@NonNull FirebaseFirestore firestore) {
        db = firestore;
    }

    @Override
    public void getCategories(@NonNull LoadRawCategoriesCallback callback) {
        CollectionReference categoriesRef = resolveCategoriesRef(callback);
        if (categoriesRef == null) {
            return;
        }
        categoriesRef
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<RawCategoryDocument> documents = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        documents.add(new RawCategoryDocument(
                                document.getId(),
                                document.getString(FIELD_TITLE),
                                document.getString(FIELD_ICON),
                                document.get(FIELD_ORDER)
                        ));
                    }
                    callback.onLoaded(documents);
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void insertCategoryAtTop(@NonNull String name,
                                    @NonNull String icon,
                                    @NonNull InsertCategoryCallback callback) {
        CollectionReference categoriesRef = resolveCategoriesRef(callback);
        if (categoriesRef == null) {
            return;
        }
        categoriesRef
                .get()
                .addOnSuccessListener(snapshot -> runInsertAndShiftBatch(snapshot, name, icon, callback))
                .addOnFailureListener(callback::onError);
    }

    @Nullable
    private CollectionReference resolveCategoriesRef(@NonNull InsertCategoryCallback callback) {
        CollectionReference categoriesRef = UserFirestorePaths.getUserCollection(db, COLLECTION_CATEGORIES);
        if (categoriesRef == null) {
            callback.onError(new IllegalStateException("User is not authenticated"));
        }
        return categoriesRef;
    }

    @Nullable
    private CollectionReference resolveCategoriesRef(@NonNull LoadRawCategoriesCallback callback) {
        CollectionReference categoriesRef = UserFirestorePaths.getUserCollection(db, COLLECTION_CATEGORIES);
        if (categoriesRef == null) {
            callback.onError(new IllegalStateException("User is not authenticated"));
        }
        return categoriesRef;
    }

    private void runInsertAndShiftBatch(@NonNull QuerySnapshot snapshot,
                                        @NonNull String name,
                                        @NonNull String icon,
                                        @NonNull InsertCategoryCallback callback) {
        CollectionReference categoriesRef = UserFirestorePaths.getUserCollection(db, COLLECTION_CATEGORIES);
        if (categoriesRef == null) {
            callback.onError(new IllegalStateException("User is not authenticated"));
            return;
        }
        WriteBatch batch = db.batch();

        for (QueryDocumentSnapshot document : snapshot) {
            DocumentReference docRef = document.getReference();
            int currentOrder = parseOrder(document.get(FIELD_ORDER));
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
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
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
}


