package com.example.ticktok.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public final class UserFirestorePaths {

    private static final String ROOT_USERS = "users";

    private UserFirestorePaths() {
    }

    @Nullable
    public static CollectionReference getUserCollection(@NonNull String collectionName) {
        return getUserCollection(FirebaseFirestore.getInstance(), collectionName);
    }

    @Nullable
    public static CollectionReference getUserCollection(@NonNull FirebaseFirestore firestore,
                                                        @NonNull String collectionName) {
        String uid = getCurrentUid();
        if (uid == null) {
            return null;
        }
        return firestore.collection(ROOT_USERS)
                .document(uid)
                .collection(collectionName);
    }

    @Nullable
    public static String getCurrentUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return null;
        }
        String uid = user.getUid();
        return uid == null || uid.trim().isEmpty() ? null : uid.trim();
    }
}

