package com.example.ticktok.activity;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticktok.R;
import com.example.ticktok.fragment.CategoryFragment;
import com.example.ticktok.adapter.MenuCategoryAdapter;
import com.example.ticktok.fragment.WelcomeFragment;
import com.example.ticktok.model.Category;
import com.example.ticktok.repository.CategoryRepository;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_SELECTED_MENU_TITLE = "state_selected_menu_title";

    private DrawerLayout drawerLayout;
    private String selectedMenuTitle;
    private CategoryRepository categoryRepository;
    private MenuCategoryAdapter categoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        selectedMenuTitle = getString(R.string.menu_welcome);
        if (savedInstanceState != null) {
            String restored = savedInstanceState.getString(STATE_SELECTED_MENU_TITLE);
            if (restored != null && !restored.trim().isEmpty()) {
                selectedMenuTitle = restored;
            }
        }

        setupInsets();
        applySystemBars();

        updateHeader(selectedMenuTitle);
        if (savedInstanceState == null) {
            showContentForMenu(selectedMenuTitle);
        }
        setupMenuButton();
        setupDrawerMenu();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCategories();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SELECTED_MENU_TITLE, selectedMenuTitle);
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

    private void applySystemBars() {
        getWindow().setStatusBarColor(android.graphics.Color.BLACK);
        getWindow().setNavigationBarColor(android.graphics.Color.BLACK);

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
    }

    private void setupMenuButton() {
        ImageButton btnMenu = findViewById(R.id.btnMenu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> openDrawerMenu());
        }
    }

    private void setupDrawerMenu() {
        categoryRepository = new CategoryRepository();

        RecyclerView rvMenuCategories = findViewById(R.id.rvMenuCategories);
        if (rvMenuCategories != null) {
            rvMenuCategories.setLayoutManager(new LinearLayoutManager(this));
            categoryAdapter = new MenuCategoryAdapter(this::onCategorySelected);
            rvMenuCategories.setAdapter(categoryAdapter);
        }

        LinearLayout btnAdd = findViewById(R.id.btnAdd);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> Toast.makeText(this, R.string.menu_add_coming_soon, Toast.LENGTH_SHORT).show());
        }

        refreshCategories();
    }

    private void refreshCategories() {
        if (categoryRepository == null || categoryAdapter == null) {
            return;
        }

        categoryRepository.getCategories(new CategoryRepository.LoadCategoriesCallback() {
            @Override
            public void onLoaded(List<Category> categories) {
                categoryAdapter.submitList(categories, selectedMenuTitle);
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(MainActivity.this, R.string.error_load_categories, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onCategorySelected(Category category) {
        selectedMenuTitle = category.getTitle();
        updateHeader(selectedMenuTitle);
        showContentForMenu(selectedMenuTitle);
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        refreshCategories();
    }

    private void updateHeader(String title) {
        TextView headerText = findViewById(R.id.headerText);
        if (headerText == null) {
            return;
        }
        if (title == null || title.trim().isEmpty()) {
            headerText.setText(getString(R.string.menu_welcome));
            return;
        }
        headerText.setText(title);
    }

    private void showContentForMenu(String title) {
        androidx.fragment.app.Fragment targetFragment;
        if (title == null || title.trim().isEmpty() || getString(R.string.menu_welcome).equalsIgnoreCase(title.trim())) {
            targetFragment = new WelcomeFragment();
        } else {
            targetFragment = CategoryFragment.newInstance(title.trim());
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFragmentContainer, targetFragment)
                .commit();
    }


    private void openDrawerMenu() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }
}