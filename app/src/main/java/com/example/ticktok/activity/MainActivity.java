package com.example.ticktok.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticktok.R;
import com.example.ticktok.adapter.MenuCategoryAdapter;
import com.example.ticktok.model.Category;
import com.example.ticktok.repository.CategoryRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private CategoryRepository categoryRepository;
    private MenuCategoryAdapter categoryAdapter;

    private boolean pendingOpenGame;
    private String pendingGameTitle;
    private String selectedMenuTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        selectedMenuTitle = getString(R.string.menu_welcome);

        setupInsets();
        getWindow().setNavigationBarColor(android.graphics.Color.BLACK);

        setupMenuButton();
        setupDrawerMenu();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCategories();
    }

    private void setupInsets() {
        drawerLayout = findViewById(R.id.main);
        if (drawerLayout == null) {
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });
    }

    private void setupMenuButton() {
        ImageButton btnMenu = findViewById(R.id.btnMenu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> openDrawerMenu());
        }
    }

    private void setupDrawerMenu() {
        categoryRepository = new CategoryRepository(this);

        RecyclerView rvMenuCategories = findViewById(R.id.rvMenuCategories);
        if (rvMenuCategories != null) {
            rvMenuCategories.setLayoutManager(new LinearLayoutManager(this));
            categoryAdapter = new MenuCategoryAdapter(this::onCategorySelected);
            rvMenuCategories.setAdapter(categoryAdapter);
        }

        LinearLayout btnAdd = findViewById(R.id.btnAdd);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> showAddCategoryDialog());
        }

        if (drawerLayout != null) {
            drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
                @Override
                public void onDrawerClosed(View drawerView) {
                    if (!pendingOpenGame) {
                        return;
                    }
                    pendingOpenGame = false;

                    Intent intent = new Intent(MainActivity.this, GameActivity.class);
                    intent.putExtra(GameActivity.EXTRA_MENU_TITLE, pendingGameTitle);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                }
            });
        }

        refreshCategories();
    }

    private void refreshCategories() {
        if (categoryAdapter == null || categoryRepository == null) {
            return;
        }
        List<Category> categories = categoryRepository.getAllCategories();
        categoryAdapter.submitList(categories, selectedMenuTitle);
    }

    private void onCategorySelected(Category category) {
        String welcomeTitle = getString(R.string.menu_welcome);
        String title = category.getTitle();

        if (welcomeTitle.equalsIgnoreCase(title)) {
            selectedMenuTitle = welcomeTitle;
            refreshCategories();
            if (drawerLayout != null) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            return;
        }

        pendingGameTitle = title;
        pendingOpenGame = true;
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra(GameActivity.EXTRA_MENU_TITLE, pendingGameTitle);
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
    }

    private void showAddCategoryDialog() {
        EditText input = new EditText(this);
        input.setHint(R.string.add_category_hint);
        input.setSingleLine(true);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.add_category_title)
                .setView(input)
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    String categoryName = input.getText() == null ? "" : input.getText().toString().trim();
                    if (categoryName.isEmpty()) {
                        Toast.makeText(this, R.string.error_empty_category, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    boolean added = categoryRepository.addCustomCategory(categoryName);
                    if (!added) {
                        Toast.makeText(this, R.string.error_duplicate_category, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    refreshCategories();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void openDrawerMenu() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }
}