package com.example.ticktok.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
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

public class GameActivity extends AppCompatActivity {

    public static final String EXTRA_MENU_TITLE = "extra_menu_title";

    private DrawerLayout drawerLayout;
    private CategoryRepository categoryRepository;
    private MenuCategoryAdapter categoryAdapter;

    private boolean pendingOpenWelcome;
    private String pendingHeaderTitle;
    private String selectedMenuTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_game);

        selectedMenuTitle = getInitialHeader();

        setupInsets();
        getWindow().setNavigationBarColor(android.graphics.Color.BLACK);

        setupHeader(selectedMenuTitle);
        setupMenuButton();
        setupDrawerMenu();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCategories();
    }

    private void setupInsets() {
        drawerLayout = findViewById(R.id.gameRoot);
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
                    if (pendingOpenWelcome) {
                        pendingOpenWelcome = false;
                        Intent intent = new Intent(GameActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        overridePendingTransition(0, 0);
                        finish();
                        return;
                    }

                    if (pendingHeaderTitle != null && !pendingHeaderTitle.isEmpty()) {
                        selectedMenuTitle = pendingHeaderTitle;
                        setupHeader(selectedMenuTitle);
                        refreshCategories();
                        pendingHeaderTitle = null;
                    }
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
        String title = category.getTitle();

        if (getString(R.string.menu_welcome).equalsIgnoreCase(title)) {
            pendingOpenWelcome = true;
            if (drawerLayout != null) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }
            return;
        }

        pendingHeaderTitle = title;
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            selectedMenuTitle = title;
            setupHeader(selectedMenuTitle);
            refreshCategories();
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

    private void setupHeader(String title) {
        TextView headerText = findViewById(R.id.headerText);
        if (headerText == null) {
            return;
        }

        if (title == null || title.trim().isEmpty()) {
            headerText.setText(getString(R.string.game_header));
            return;
        }
        headerText.setText(title);
    }

    private String getInitialHeader() {
        String selectedTitle = getIntent().getStringExtra(EXTRA_MENU_TITLE);
        if (selectedTitle == null || selectedTitle.trim().isEmpty()) {
            return getString(R.string.game_header);
        }
        return selectedTitle;
    }

    private void openDrawerMenu() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }
}
