package com.example.ticktok.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.ticktok.R;
import com.example.ticktok.fragment.AddCategoryFragment;
import com.example.ticktok.fragment.CategoryFragment;
import com.example.ticktok.fragment.AddTaskBottomSheetFragment;
import com.example.ticktok.fragment.AddEventBottomSheetFragment;
import com.example.ticktok.adapter.MenuCategoryAdapter;
import com.example.ticktok.fragment.CalendarFragment;
import com.example.ticktok.fragment.EventFragment;
import com.example.ticktok.fragment.PomodoroFragment;
import com.example.ticktok.fragment.SearchFragment;
import com.example.ticktok.fragment.WelcomeFragment;
import com.example.ticktok.model.Category;
import com.example.ticktok.repository.CategoryRepository;
import com.example.ticktok.repository.CategoryRepositoryContract;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_SELECTED_MENU_TITLE = "state_selected_menu_title";
    private static final String TAG_ADD_TASK_SHEET = "add_task_sheet";
    private static final String TAG_ADD_EVENT_SHEET = "add_event_sheet";
    private static final String TAG_ADD_CATEGORY_SHEET = "add_category_sheet";

    private DrawerLayout drawerLayout;
    private String selectedMenuTitle;
    private String selectedCategoryId;
    private CategoryRepositoryContract categoryRepository;
    private MenuCategoryAdapter categoryAdapter;
    private final List<Category> menuCategories = new ArrayList<>();
    private FloatingActionButton sharedFab;
    private boolean isPomodoroScreenActive;
    private boolean isSearchScreenActive;
    private ImageView dockIcon1;
    private ImageView dockIcon2;
    private ImageView dockIcon3;
    private ImageView dockIcon4;
    private ImageView dockIcon5;

    private static final class ScreenState {
        final boolean isPomodoro;
        final boolean isSearch;
        final boolean isCalendar;
        final boolean isEvent;

        ScreenState(boolean isPomodoro, boolean isSearch, boolean isCalendar, boolean isEvent) {
            this.isPomodoro = isPomodoro;
            this.isSearch = isSearch;
            this.isCalendar = isCalendar;
            this.isEvent = isEvent;
        }
    }

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
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        refreshCategories();
        syncFabVisibility();
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

        setupProfileHeader();

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

    private void setupProfileHeader() {
        LinearLayout layoutUserProfile = findViewById(R.id.layoutUserProfile);
        TextView tvUserName = findViewById(R.id.tvUserName);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (tvUserName != null) {
            tvUserName.setText(resolveDisplayName(user));
        }

        if (layoutUserProfile != null) {
            layoutUserProfile.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
            });
        }
    }

    @NonNull
    private String resolveDisplayName(@Nullable FirebaseUser user) {
        if (user == null) {
            return getString(R.string.menu_user_default_name);
        }

        if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            return user.getDisplayName().trim();
        }

        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            return user.getEmail().trim();
        }

        return getString(R.string.menu_user_default_name);
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
        sharedFab.setOnClickListener(v -> openAddSheetForCurrentScreen());
        syncFabVisibility();
    }

    private void openAddSheetForCurrentScreen() {
        ScreenState state = evaluateScreenState(normalizeTitle(selectedMenuTitle));
        if (state.isEvent) {
            openAddEventBottomSheet();
            return;
        }
        openAddTaskBottomSheet();
    }

    private void syncFabVisibility() {
        updateFabVisibility(isAddCategorySheetShowing() || isPomodoroScreenActive || isSearchScreenActive);
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

        Long prefillDueDate = null;
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.contentFragmentContainer);
        if (currentFragment instanceof CalendarFragment) {
            prefillDueDate = ((CalendarFragment) currentFragment).getSelectedDateMillisForTask();
        }

        AddTaskBottomSheetFragment sheet = new AddTaskBottomSheetFragment(selectedCategoryId, prefillDueDate);
        sheet.show(getSupportFragmentManager(), TAG_ADD_TASK_SHEET);
    }

    private void openAddEventBottomSheet() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.contentFragmentContainer);
        if (!(currentFragment instanceof EventFragment)) {
            return;
        }
        if (getSupportFragmentManager().findFragmentByTag(TAG_ADD_EVENT_SHEET) != null) {
            return;
        }
        AddEventBottomSheetFragment sheet = new AddEventBottomSheetFragment();
        sheet.show(getSupportFragmentManager(), TAG_ADD_EVENT_SHEET);
    }

    private void refreshCategories() {
        if (categoryRepository == null || categoryAdapter == null) {
            return;
        }

        categoryRepository.getCategories(new CategoryRepositoryContract.LoadCategoriesCallback() {
            @Override
            public void onLoaded(List<Category> categories) {
                menuCategories.clear();
                menuCategories.addAll(categories);
                selectedCategoryId = resolveCategoryIdForTitle(normalizeTitle(selectedMenuTitle));
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
        selectedCategoryId = category.getId();
        navigateTo(category.getTitle(), true, true);
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
        String normalizedTitle = normalizeTitle(title);
        Fragment targetFragment = resolveFragmentForTitle(normalizedTitle);

        applyScreenChrome(normalizedTitle);
        updateDockSelection(normalizedTitle);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFragmentContainer, targetFragment)
                .commit();
    }

    private void applyScreenChrome(String title) {
        applyScreenChrome(evaluateScreenState(normalizeTitle(title)));
    }

    private void applyScreenChrome(ScreenState state) {
        isPomodoroScreenActive = state.isPomodoro;
        isSearchScreenActive = state.isSearch;

        View topBar = findViewById(R.id.topBar);
        TextView headerText = findViewById(R.id.headerText);

        if (topBar != null) {
            topBar.setVisibility((state.isPomodoro || state.isSearch || state.isCalendar || state.isEvent) ? View.GONE : View.VISIBLE);
        }
        if (headerText != null) {
            headerText.setVisibility((state.isPomodoro || state.isCalendar) ? View.GONE : View.VISIBLE);
        }

        if (drawerLayout != null) {
            drawerLayout.setDrawerLockMode((state.isPomodoro || state.isCalendar || state.isEvent)
                    ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED
                    : DrawerLayout.LOCK_MODE_UNLOCKED);
            if (state.isPomodoro || state.isCalendar || state.isEvent) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
        }

        updateContentTopConstraint(state.isPomodoro || state.isCalendar);

        syncFabVisibility();
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
        ScreenState state = evaluateScreenState(normalizeTitle(title));
        int unselectedColor = ContextCompat.getColor(this, R.color.text_white);
        int selectedColor = ContextCompat.getColor(this, R.color.fab_orange);

        setDockIconState(dockIcon1, false, unselectedColor);
        setDockIconState(dockIcon2, false, unselectedColor);
        setDockIconState(dockIcon3, false, unselectedColor);
        setDockIconState(dockIcon4, false, unselectedColor);
        setDockIconState(dockIcon5, false, unselectedColor);

        if (state.isPomodoro) {
            setDockIconState(dockIcon1, true, selectedColor);
        } else if (state.isSearch) {
            setDockIconState(dockIcon2, true, selectedColor);
        } else if (state.isCalendar) {
            setDockIconState(dockIcon4, true, selectedColor);
        } else if (state.isEvent) {
            setDockIconState(dockIcon5, true, selectedColor);
        } else {
            setDockIconState(dockIcon3, true, selectedColor);
        }
    }

    private void navigateTo(String title, boolean closeDrawer, boolean refreshCategoryList) {
        selectedMenuTitle = normalizeTitle(title);
        selectedCategoryId = resolveCategoryIdForTitle(selectedMenuTitle);
        updateHeader(selectedMenuTitle);
        showContentForMenu(selectedMenuTitle);
        if (closeDrawer && drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        if (refreshCategoryList) {
            refreshCategories();
        }
    }

    private String normalizeTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return getString(R.string.menu_welcome);
        }
        return title.trim();
    }

    private ScreenState evaluateScreenState(String normalizedTitle) {
        return new ScreenState(
                getString(R.string.menu_pomodoro).equalsIgnoreCase(normalizedTitle),
                getString(R.string.menu_search).equalsIgnoreCase(normalizedTitle),
                getString(R.string.menu_calendar).equalsIgnoreCase(normalizedTitle),
                getString(R.string.menu_event).equalsIgnoreCase(normalizedTitle)
        );
    }

    private Fragment resolveFragmentForTitle(String normalizedTitle) {
        ScreenState state = evaluateScreenState(normalizedTitle);
        if (getString(R.string.menu_welcome).equalsIgnoreCase(normalizedTitle)) {
            return new WelcomeFragment();
        }
        if (state.isSearch) {
            return new SearchFragment();
        }
        if (state.isPomodoro) {
            return new PomodoroFragment();
        }
        if (state.isCalendar) {
            return new CalendarFragment();
        }
        if (state.isEvent) {
            return new EventFragment();
        }
        return CategoryFragment.newInstance(normalizedTitle, resolveCategoryIdForTitle(normalizedTitle));
    }

    @Nullable
    private String resolveCategoryIdForTitle(String normalizedTitle) {
        ScreenState state = evaluateScreenState(normalizedTitle);
        if (getString(R.string.menu_welcome).equalsIgnoreCase(normalizedTitle)
                || state.isSearch
                || state.isPomodoro
                || state.isCalendar
                || state.isEvent) {
            return null;
        }
        for (Category category : menuCategories) {
            if (category.getTitle() != null && category.getTitle().trim().equalsIgnoreCase(normalizedTitle)) {
                return category.getId();
            }
        }
        return null;
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
        if (isAddCategorySheetShowing()) {
            return;
        }
        updateFabVisibility(true);

        AddCategoryFragment sheet = new AddCategoryFragment();
        sheet.show(getSupportFragmentManager(), TAG_ADD_CATEGORY_SHEET);
    }

    public void onAddCategorySheetDismissed() {
        syncFabVisibility();
        View root = findViewById(android.R.id.content);
        if (root != null) {
            root.post(this::syncFabVisibility);
        }
    }

    private boolean isAddCategorySheetShowing() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_ADD_CATEGORY_SHEET);
        if (fragment instanceof DialogFragment) {
            android.app.Dialog dialog = ((DialogFragment) fragment).getDialog();
            return dialog != null && dialog.isShowing();
        }
        return fragment != null && fragment.isVisible();
    }

    private void openPomodoroScreen() {
        navigateTo(getString(R.string.menu_pomodoro), true, false);
    }

    private void openHomeScreen() {
        navigateTo(getString(R.string.menu_welcome), true, false);
    }

    private void openSearchScreen() {
        navigateTo(getString(R.string.menu_search), true, false);
    }

    private void openCalendarScreen() {
        navigateTo(getString(R.string.menu_calendar), true, false);
    }

    private void openEventScreen() {
        navigateTo(getString(R.string.menu_event), true, false);
    }


    private void openDrawerMenu() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }
}