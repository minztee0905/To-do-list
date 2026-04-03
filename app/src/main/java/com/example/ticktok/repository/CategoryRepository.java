package com.example.ticktok.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.ticktok.R;
import com.example.ticktok.model.Category;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CategoryRepository {

    private static final String PREF_NAME = "category_repo";
    private static final String KEY_CUSTOM_CATEGORIES = "custom_categories";

    private final SharedPreferences prefs;
    private final Context context;

    public CategoryRepository(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        categories.add(new Category("today", "📚", context.getString(R.string.menu_today), true));
        categories.add(new Category("inbox", "📬", context.getString(R.string.menu_inbox), true));
        categories.add(new Category("game", "🎮", context.getString(R.string.menu_game), true));
        categories.add(new Category("welcome", "👋", context.getString(R.string.menu_welcome), true));
        categories.add(new Category("work", "💼", context.getString(R.string.menu_work), true));
        categories.add(new Category("personal", "🏠", context.getString(R.string.menu_personal), true));
        categories.add(new Category("shopping", "📦", context.getString(R.string.menu_shopping), true));
        categories.add(new Category("learning", "📖", context.getString(R.string.menu_learning), true));
        categories.add(new Category("wish_list", "🦄", context.getString(R.string.menu_wish_list), true));
        categories.add(new Category("fitness", "🏃", context.getString(R.string.menu_fitness), true));

        Set<String> customTitles = readCustomTitles();
        int index = 0;
        for (String title : customTitles) {
            String id = "custom_" + index;
            categories.add(new Category(id, "✨", title, false));
            index++;
        }
        return categories;
    }

    public boolean addCustomCategory(String title) {
        String normalized = normalize(title);
        if (normalized.isEmpty() || isDefaultTitle(normalized)) {
            return false;
        }

        Set<String> titles = readCustomTitles();
        for (String existing : titles) {
            if (existing.equalsIgnoreCase(normalized)) {
                return false;
            }
        }

        titles.add(normalized);
        saveCustomTitles(titles);
        return true;
    }

    private boolean isDefaultTitle(String title) {
        List<Category> defaults = getAllDefaultCategories();
        for (Category category : defaults) {
            if (category.getTitle().toLowerCase(Locale.ROOT).equals(title.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private List<Category> getAllDefaultCategories() {
        List<Category> defaults = new ArrayList<>();
        defaults.add(new Category("today", "📚", context.getString(R.string.menu_today), true));
        defaults.add(new Category("inbox", "📬", context.getString(R.string.menu_inbox), true));
        defaults.add(new Category("game", "🎮", context.getString(R.string.menu_game), true));
        defaults.add(new Category("welcome", "👋", context.getString(R.string.menu_welcome), true));
        defaults.add(new Category("work", "💼", context.getString(R.string.menu_work), true));
        defaults.add(new Category("personal", "🏠", context.getString(R.string.menu_personal), true));
        defaults.add(new Category("shopping", "📦", context.getString(R.string.menu_shopping), true));
        defaults.add(new Category("learning", "📖", context.getString(R.string.menu_learning), true));
        defaults.add(new Category("wish_list", "🦄", context.getString(R.string.menu_wish_list), true));
        defaults.add(new Category("fitness", "🏃", context.getString(R.string.menu_fitness), true));
        return defaults;
    }

    private Set<String> readCustomTitles() {
        String raw = prefs.getString(KEY_CUSTOM_CATEGORIES, "");
        Set<String> result = new LinkedHashSet<>();
        if (raw == null || raw.trim().isEmpty()) {
            return result;
        }

        String[] parts = raw.split("\\n");
        for (String part : parts) {
            String normalized = normalize(part);
            if (!normalized.isEmpty()) {
                result.add(normalized);
            }
        }
        return result;
    }

    private void saveCustomTitles(Set<String> titles) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (String title : titles) {
            if (i > 0) {
                builder.append("\n");
            }
            builder.append(title);
            i++;
        }
        prefs.edit().putString(KEY_CUSTOM_CATEGORIES, builder.toString()).apply();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}

