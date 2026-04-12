package com.example.ticktok.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentContainerView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticktok.R;
import com.example.ticktok.fragment.AddCategoryFragment;
import com.example.ticktok.fragment.CategoryFragment;
import com.example.ticktok.fragment.AddTaskBottomSheetFragment;
import com.example.ticktok.adapter.MenuCategoryAdapter;
import com.example.ticktok.fragment.CalendarFragment;
import com.example.ticktok.fragment.EventFragment;
import com.example.ticktok.fragment.PomodoroFragment;
import com.example.ticktok.fragment.SearchFragment;
import com.example.ticktok.fragment.WelcomeFragment;
import com.example.ticktok.model.Category;
import com.example.ticktok.repository.CategoryRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_SELECTED_MENU_TITLE = "state_selected_menu_title";
    private static final String TAG_ADD_TASK_SHEET = "add_task_sheet";
    private static final String TAG_ADD_CATEGORY_SCREEN = "add_category_screen";
    private static final String BACKSTACK_ADD_CATEGORY = "add_category";

    private DrawerLayout drawerLayout;
    private String selectedMenuTitle;
    private CategoryRepository categoryRepository;
    private MenuCategoryAdapter categoryAdapter;
    private final List<Category> menuCategories = new ArrayList<>();
    private FragmentContainerView addCategoryOverlayContainer;
    private FloatingActionButton sharedFab;
    private boolean wasShowingAddCategory;
    private boolean isPomodoroScreenActive;
    private boolean isSearchScreenActive;
    private ImageView dockIcon1;
    private ImageView dockIcon2;
    private ImageView dockIcon3;
    private ImageView dockIcon4;
    private ImageView dockIcon5;

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
        setupAddCategoryOverlay();
        applyScreenChrome(selectedMenuTitle);

        updateHeader(selectedMenuTitle);
        if (savedInstanceState == null) {
            showContentForMenu(selectedMenuTitle);
        }
        setupMenuButton();
        setupDrawerMenu();
        setupAddCategoryResultListener();
        setupSharedFab();
        setupDockNavigation();
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

    private void setupAddCategoryOverlay() {
        addCategoryOverlayContainer = findViewById(R.id.addCategoryOverlayContainer);
        getSupportFragmentManager().addOnBackStackChangedListener(this::updateAddCategoryOverlayVisibility);
        wasShowingAddCategory = false;
        updateAddCategoryOverlayVisibility();
    }

    private void updateAddCategoryOverlayVisibility() {
        if (addCategoryOverlayContainer == null) {
            return;
        }

        boolean isShowingAddCategory = getSupportFragmentManager()
                .findFragmentByTag(TAG_ADD_CATEGORY_SCREEN) != null;

        addCategoryOverlayContainer.setVisibility(isShowingAddCategory ? View.VISIBLE : View.GONE);
        updateFabVisibility(isShowingAddCategory || isPomodoroScreenActive || isSearchScreenActive);

        if (wasShowingAddCategory && !isShowingAddCategory && drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
        wasShowingAddCategory = isShowingAddCategory;
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
            btnAdd.setOnClickListener(v -> openAddCategoryScreen());
        }

        refreshCategories();
    }

    private void setupDockNavigation() {
        dockIcon1 = findViewById(R.id.dockIcon1);
        if (dockIcon1 != null) {
            dockIcon1.setOnClickListener(v -> openPomodoroScreen());
        }

        dockIcon3 = findViewById(R.id.dockIcon3);
        if (dockIcon3 != null) {
            dockIcon3.setOnClickListener(v -> openHomeScreen());
        }

        dockIcon2 = findViewById(R.id.dockIcon2);
        if (dockIcon2 != null) {
            dockIcon2.setOnClickListener(v -> openSearchScreen());
        }

        dockIcon4 = findViewById(R.id.dockIcon4);
        if (dockIcon4 != null) {
            dockIcon4.setOnClickListener(v -> openCalendarScreen());
        }

        dockIcon5 = findViewById(R.id.dockIcon5);
        if (dockIcon5 != null) {
            dockIcon5.setOnClickListener(v -> openEventScreen());
        }

        updateDockSelection(selectedMenuTitle);
    }

    private void setupSharedFab() {
        sharedFab = findViewById(R.id.fab);
        if (sharedFab == null) {
            return;
        }
        sharedFab.setOnClickListener(v -> openAddTaskBottomSheet());
        updateFabVisibility(getSupportFragmentManager().findFragmentByTag(TAG_ADD_CATEGORY_SCREEN) != null);
    }

    private void updateFabVisibility(boolean hideFab) {
        if (sharedFab == null) {
            return;
        }
        sharedFab.setVisibility(hideFab ? View.GONE : View.VISIBLE);
    }

    private void openAddTaskBottomSheet() {
        if (getSupportFragmentManager().findFragmentByTag(TAG_ADD_TASK_SHEET) != null) {
            return;
        }
        AddTaskBottomSheetFragment sheet = new AddTaskBottomSheetFragment();
        sheet.show(getSupportFragmentManager(), TAG_ADD_TASK_SHEET);
    }

    private void refreshCategories() {
        if (categoryRepository == null || categoryAdapter == null) {
            return;
        }

        categoryRepository.getCategories(new CategoryRepository.LoadCategoriesCallback() {
            @Override
            public void onLoaded(List<Category> categories) {
                menuCategories.clear();
                menuCategories.addAll(categories);
                categoryAdapter.submitList(new ArrayList<>(menuCategories), selectedMenuTitle);
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(MainActivity.this, R.string.error_load_categories, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAddCategoryResultListener() {
        getSupportFragmentManager().setFragmentResultListener(
                AddCategoryFragment.RESULT_KEY_ADD_CATEGORY,
                this,
                (requestKey, result) -> refreshCategories()
        );
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
        } else if (getString(R.string.menu_search).equalsIgnoreCase(title.trim())) {
            targetFragment = new SearchFragment();
        } else if (getString(R.string.menu_pomodoro).equalsIgnoreCase(title.trim())) {
            targetFragment = new PomodoroFragment();
        } else if (getString(R.string.menu_calendar).equalsIgnoreCase(title.trim())) {
            targetFragment = new CalendarFragment();
        } else if (getString(R.string.menu_event).equalsIgnoreCase(title.trim())) {
            targetFragment = new EventFragment();
        } else {
            targetFragment = CategoryFragment.newInstance(title.trim());
        }

        applyScreenChrome(title);
        updateDockSelection(title);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFragmentContainer, targetFragment)
                .commit();
    }

    private void applyScreenChrome(String title) {
        boolean isPomodoro = title != null && getString(R.string.menu_pomodoro).equalsIgnoreCase(title.trim());
        boolean isSearch = title != null && getString(R.string.menu_search).equalsIgnoreCase(title.trim());
        boolean isCalendar = title != null && getString(R.string.menu_calendar).equalsIgnoreCase(title.trim());
        boolean isEvent = title != null && getString(R.string.menu_event).equalsIgnoreCase(title.trim());
        isPomodoroScreenActive = isPomodoro;
        isSearchScreenActive = isSearch;

        View topBar = findViewById(R.id.topBar);
        TextView headerText = findViewById(R.id.headerText);

        if (topBar != null) {
            topBar.setVisibility((isPomodoro || isSearch || isCalendar || isEvent) ? View.GONE : View.VISIBLE);
        }
        if (headerText != null) {
            headerText.setVisibility((isPomodoro || isCalendar) ? View.GONE : View.VISIBLE);
        }

        if (drawerLayout != null) {
            drawerLayout.setDrawerLockMode((isPomodoro || isCalendar || isEvent)
                    ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED
                    : DrawerLayout.LOCK_MODE_UNLOCKED);
            if (isPomodoro || isCalendar || isEvent) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
        }

        updateContentTopConstraint(isPomodoro || isCalendar);

        boolean isShowingAddCategory = getSupportFragmentManager().findFragmentByTag(TAG_ADD_CATEGORY_SCREEN) != null;
        updateFabVisibility(isPomodoro || isSearch || isShowingAddCategory);
    }

    private void updateContentTopConstraint(boolean isPomodoro) {
        ConstraintLayout contentLayout = findViewById(R.id.contentLayout);
        if (contentLayout == null) {
            return;
        }

        ConstraintSet set = new ConstraintSet();
        set.clone(contentLayout);

        set.clear(R.id.contentFragmentContainer, ConstraintSet.TOP);
        if (isPomodoro) {
            set.connect(R.id.contentFragmentContainer, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0);
        } else {
            set.connect(R.id.contentFragmentContainer, ConstraintSet.TOP, R.id.headerText, ConstraintSet.BOTTOM, dpToPx(12));
        }
        set.applyTo(contentLayout);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void updateDockSelection(String title) {
        int unselectedColor = ContextCompat.getColor(this, R.color.text_white);
        int selectedColor = ContextCompat.getColor(this, R.color.fab_orange);

        setDockIconState(dockIcon1, false, unselectedColor);
        setDockIconState(dockIcon2, false, unselectedColor);
        setDockIconState(dockIcon3, false, unselectedColor);
        setDockIconState(dockIcon4, false, unselectedColor);
        setDockIconState(dockIcon5, false, unselectedColor);

        boolean isPomodoro = title != null && getString(R.string.menu_pomodoro).equalsIgnoreCase(title.trim());
        boolean isSearch = title != null && getString(R.string.menu_search).equalsIgnoreCase(title.trim());
        boolean isCalendar = title != null && getString(R.string.menu_calendar).equalsIgnoreCase(title.trim());
        boolean isEvent = title != null && getString(R.string.menu_event).equalsIgnoreCase(title.trim());
        if (isPomodoro) {
            setDockIconState(dockIcon1, true, selectedColor);
        } else if (isSearch) {
            setDockIconState(dockIcon2, true, selectedColor);
        } else if (isCalendar) {
            setDockIconState(dockIcon4, true, selectedColor);
        } else if (isEvent) {
            setDockIconState(dockIcon5, true, selectedColor);
        } else {
            setDockIconState(dockIcon3, true, selectedColor);
        }
    }

    private void setDockIconState(ImageView icon, boolean isSelected, int color) {
        if (icon == null) {
            return;
        }
        icon.setColorFilter(color);
        float targetScale = isSelected ? 1.18f : 1f;
        float targetAlpha = isSelected ? 1f : 0.6f;
        icon.animate()
                .scaleX(targetScale)
                .scaleY(targetScale)
                .alpha(targetAlpha)
                .setDuration(160L)
                .start();
    }

    private void openAddCategoryScreen() {
        if (getSupportFragmentManager().findFragmentByTag(TAG_ADD_CATEGORY_SCREEN) != null) {
            return;
        }

        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        if (addCategoryOverlayContainer != null) {
            addCategoryOverlayContainer.setVisibility(View.VISIBLE);
        }
        updateFabVisibility(true);

        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_bottom,
                        R.anim.slide_out_bottom,
                        R.anim.slide_in_bottom,
                        R.anim.slide_out_bottom
                )
                .replace(R.id.addCategoryOverlayContainer, new AddCategoryFragment(), TAG_ADD_CATEGORY_SCREEN)
                .addToBackStack(BACKSTACK_ADD_CATEGORY)
                .commit();
    }

    private void openPomodoroScreen() {
        selectedMenuTitle = getString(R.string.menu_pomodoro);
        updateHeader(selectedMenuTitle);
        showContentForMenu(selectedMenuTitle);
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    private void openHomeScreen() {
        selectedMenuTitle = getString(R.string.menu_welcome);
        updateHeader(selectedMenuTitle);
        showContentForMenu(selectedMenuTitle);
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    private void openSearchScreen() {
        selectedMenuTitle = getString(R.string.menu_search);
        updateHeader(selectedMenuTitle);
        showContentForMenu(selectedMenuTitle);
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    private void openCalendarScreen() {
        selectedMenuTitle = getString(R.string.menu_calendar);
        updateHeader(selectedMenuTitle);
        showContentForMenu(selectedMenuTitle);
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    private void openEventScreen() {
        selectedMenuTitle = getString(R.string.menu_event);
        updateHeader(selectedMenuTitle);
        showContentForMenu(selectedMenuTitle);
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }


    private void openDrawerMenu() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }
}