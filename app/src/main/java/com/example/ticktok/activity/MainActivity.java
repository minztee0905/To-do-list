package com.example.ticktok.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

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
import com.example.ticktok.util.UserFirestorePaths;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_SELECTED_MENU_TITLE = "state_selected_menu_title";
    private static final String TAG_ADD_TASK_SHEET = "add_task_sheet";
    private static final String TAG_ADD_EVENT_SHEET = "add_event_sheet";
    private static final String TAG_ADD_CATEGORY_SHEET = "add_category_sheet";

    private static final String PREFS_TASK_CLEANUP = "prefs_task_cleanup";
    private static final String PREF_KEY_LAST_CLEANUP_DAY_PREFIX = "last_cleanup_day_";

    private DrawerLayout drawerLayout;
    @Nullable
    private ImageButton btnMenu;
    private String selectedMenuTitle;
    private String selectedCategoryId;
    private CategoryRepositoryContract categoryRepository;
    private MenuCategoryAdapter categoryAdapter;
    private ItemTouchHelper categoryItemTouchHelper;
    private final List<Category> menuCategories = new ArrayList<>();
    private FloatingActionButton sharedFab;
    private boolean isPomodoroScreenActive;
    private boolean isSearchScreenActive;
    private ImageView dockIcon1;
    private ImageView dockIcon2;
    private ImageView dockIcon3;
    private ImageView dockIcon4;
    private ImageView dockIcon5;

    private boolean showingBackButton;

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
        getSupportFragmentManager().addOnBackStackChangedListener(this::updateTopLeftNavigationButton);
        setupDrawerMenu();
        setupAddCategoryResultListener();
        setupSharedFab();
        setupDockNavigation();

        // Ensure the correct icon is set after initial fragment transaction.
        updateTopLeftNavigationButton();
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

        // Delete tasks that were completed on previous days (run at most once per day).
        maybeRunDailyCompletedTaskCleanup();

        refreshCategories();
        syncFabVisibility();
    }

    private void maybeRunDailyCompletedTaskCleanup() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        String uid = user.getUid();
        String todayKey = getTodayKey();

        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_TASK_CLEANUP, MODE_PRIVATE);
        String lastKey = prefs.getString(PREF_KEY_LAST_CLEANUP_DAY_PREFIX + uid, "");
        if (todayKey.equals(lastKey)) {
            return;
        }

        long todayStartMillis = normalizeToStartOfDay(System.currentTimeMillis());
        Date todayStartDate = new Date(todayStartMillis);

        CollectionReference tasksRef = UserFirestorePaths.getUserCollection("tasks");
        if (tasksRef == null) {
            return;
        }

        Set<String> deletedIds = new HashSet<>();
        deleteCompletedTasksBefore(tasksRef, todayStartDate, deletedIds, () -> {
            // Extra safety: purge older completed tasks that might miss `completedAt`.
            purgeCompletedTasksWithoutTimestamp(tasksRef, "completed", todayStartDate, deletedIds, () ->
                    purgeCompletedTasksWithoutTimestamp(tasksRef, "isCompleted", todayStartDate, deletedIds, () ->
                            prefs.edit().putString(PREF_KEY_LAST_CLEANUP_DAY_PREFIX + uid, todayKey).apply()
                    )
            );
        });
    }

    @NonNull
    private String getTodayKey() {
        Calendar c = Calendar.getInstance();
        return String.format(Locale.US, "%04d%02d%02d",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH));
    }

    private long normalizeToStartOfDay(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private interface VoidCallback {
        void onDone();
    }

    private void deleteCompletedTasksBefore(@NonNull CollectionReference tasksRef,
                                           @NonNull Date todayStart,
                                           @NonNull Set<String> deletedIds,
                                           @NonNull VoidCallback onDone) {
        // Query only by `completedAt` to avoid composite indexes.
        Query q = tasksRef
                .whereLessThan("completedAt", todayStart)
                .orderBy("completedAt")
                .limit(450);

        q.get()
                .addOnSuccessListener(snapshot -> {
                    List<DocumentSnapshot> docs = snapshot.getDocuments();
                    if (docs.isEmpty()) {
                        onDone.onDone();
                        return;
                    }

                    WriteBatch batch = FirebaseFirestore.getInstance().batch();
                    for (DocumentSnapshot doc : docs) {
                        batch.delete(doc.getReference());
                        deletedIds.add(doc.getId());
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> deleteCompletedTasksBefore(tasksRef, todayStart, deletedIds, onDone))
                            .addOnFailureListener(error -> {
                                // If we fail (offline, permission, etc.), don't mark prefs.
                            });
                })
                .addOnFailureListener(error -> {
                    // Don't mark prefs on failure.
                });
    }

    private void purgeCompletedTasksWithoutTimestamp(@NonNull CollectionReference tasksRef,
                                                     @NonNull String completedField,
                                                     @NonNull Date todayStart,
                                                     @NonNull Set<String> deletedIds,
                                                     @NonNull VoidCallback onDone) {
        // Some older docs might have boolean completion set but missing completedAt.
        tasksRef.whereEqualTo(completedField, true)
                .limit(450)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<DocumentSnapshot> docs = snapshot.getDocuments();
                    if (docs.isEmpty()) {
                        onDone.onDone();
                        return;
                    }

                    List<DocumentSnapshot> toDelete = new ArrayList<>();
                    for (DocumentSnapshot doc : docs) {
                        if (deletedIds.contains(doc.getId())) {
                            continue;
                        }
                        Date completedAt = doc.getDate("completedAt");
                        if (completedAt == null || completedAt.before(todayStart)) {
                            toDelete.add(doc);
                        }
                    }

                    if (toDelete.isEmpty()) {
                        // Nothing to delete in this batch; assume remaining are today-completed.
                        onDone.onDone();
                        return;
                    }

                    WriteBatch batch = FirebaseFirestore.getInstance().batch();
                    for (DocumentSnapshot doc : toDelete) {
                        batch.delete(doc.getReference());
                        deletedIds.add(doc.getId());
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> purgeCompletedTasksWithoutTimestamp(tasksRef, completedField, todayStart, deletedIds, onDone))
                            .addOnFailureListener(error -> {
                                // Don't mark prefs on failure.
                            });
                })
                .addOnFailureListener(error -> {
                    // Don't mark prefs on failure.
                });
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
        btnMenu = findViewById(R.id.btnMenu);
        if (btnMenu == null) {
            return;
        }

        btnMenu.setOnClickListener(v -> {
            if (shouldShowBackButton()) {
                getOnBackPressedDispatcher().onBackPressed();
            } else {
                openDrawerMenu();
            }
        });

        updateTopLeftNavigationButton();
    }

    private boolean shouldShowBackButton() {
        return getSupportFragmentManager().getBackStackEntryCount() > 0;
    }

    private void updateTopLeftNavigationButton() {
        if (btnMenu == null) {
            return;
        }

        boolean showBack = shouldShowBackButton();
        if (showBack == showingBackButton) {
            return;
        }
        showingBackButton = showBack;

        btnMenu.setImageResource(showBack ? R.drawable.home_ic_back : R.drawable.home_ic_menu_more);
        btnMenu.setContentDescription(getString(showBack ? R.string.back_button : R.string.menu_button));
    }

    private void setupDrawerMenu() {
        categoryRepository = new CategoryRepository();

        setupProfileHeader();

        RecyclerView rvMenuCategories = findViewById(R.id.rvMenuCategories);
        if (rvMenuCategories != null) {
            rvMenuCategories.setLayoutManager(new LinearLayoutManager(this));
            categoryAdapter = new MenuCategoryAdapter(
                    this::onCategorySelected,
                    this::onCategoryLongPressed,
                    viewHolder -> {
                        if (categoryItemTouchHelper != null) {
                            categoryItemTouchHelper.startDrag(viewHolder);
                        }
                    }
            );
            rvMenuCategories.setAdapter(categoryAdapter);

            ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                    0
            ) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView,
                                      @NonNull RecyclerView.ViewHolder viewHolder,
                                      @NonNull RecyclerView.ViewHolder target) {
                    if (categoryAdapter == null) {
                        return false;
                    }
                    int from = viewHolder.getBindingAdapterPosition();
                    int to = target.getBindingAdapterPosition();

                    // Position 0 is reserved for the fixed Welcome category.
                    if (from == 0 || to == 0) {
                        return false;
                    }
                    categoryAdapter.moveItem(from, to);
                    return true;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    // no-op (we don't support swipe)
                }

                @Override
                public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                    super.clearView(recyclerView, viewHolder);
                    persistMenuCategoryOrder();
                }

                @Override
                public boolean isLongPressDragEnabled() {
                    // Drag is started via the handle to avoid conflict with long-press edit/delete.
                    return false;
                }
            };

            categoryItemTouchHelper = new ItemTouchHelper(callback);
            categoryItemTouchHelper.attachToRecyclerView(rvMenuCategories);
        }

        LinearLayout btnAdd = findViewById(R.id.btnAdd);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> openAddCategoryScreen());
        }

        refreshCategories();
    }

    private void persistMenuCategoryOrder() {
        if (categoryRepository == null || categoryAdapter == null) {
            return;
        }
        List<Category> items = categoryAdapter.getCurrentItems();
        if (items.isEmpty()) {
            return;
        }

        // Skip the fixed Welcome category (virtual, not stored in Firestore).
        List<Category> mutable = new ArrayList<>();
        for (Category c : items) {
            if (c != null && !Category.ID_WELCOME.equals(c.getId())) {
                mutable.add(c);
            }
        }
        if (mutable.isEmpty()) {
            return;
        }

        for (int i = 0; i < mutable.size(); i++) {
            mutable.get(i).setOrder(i + 1);
        }

        categoryRepository.updateCategoryOrders(mutable, new CategoryRepositoryContract.OnCategorySavedListener() {
            @Override
            public void onSuccess() {
                // Keep local cache consistent and refresh to ensure ordering is applied.
                menuCategories.clear();
                menuCategories.addAll(mutable);
                refreshCategories();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MainActivity.this, R.string.error_save_category_order, Toast.LENGTH_SHORT).show();
            }
        });
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

                // Keep selectedCategoryId stable if we are currently on a category screen.
                if (selectedCategoryId == null || selectedCategoryId.trim().isEmpty()) {
                    selectedCategoryId = resolveCategoryIdForTitle(normalizeTitle(selectedMenuTitle));
                }

                // Always show Welcome as the first (fixed) item.
                List<Category> display = new ArrayList<>();
                display.add(createWelcomeDrawerCategory());
                display.addAll(menuCategories);

                String highlightId = selectedCategoryId;
                if ((highlightId == null || highlightId.trim().isEmpty())
                        && getString(R.string.menu_welcome).equalsIgnoreCase(normalizeTitle(selectedMenuTitle))) {
                    highlightId = Category.ID_WELCOME;
                }

                categoryAdapter.submitList(display, selectedMenuTitle, highlightId);
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
                (requestKey, result) -> {
                    String updatedCategoryId = result.getString(AddCategoryFragment.RESULT_KEY_CATEGORY_ID);
                    String updatedName = result.getString(AddCategoryFragment.RESULT_KEY_CATEGORY_NAME);
                    if (updatedCategoryId != null
                            && updatedCategoryId.equals(selectedCategoryId)
                            && updatedName != null
                            && !updatedName.trim().isEmpty()) {
                        selectedMenuTitle = updatedName.trim();
                        updateHeader(selectedMenuTitle);
                    }
                    refreshCategories();
                }
        );
    }

    private void onCategorySelected(Category category) {
        if (category == null) {
            return;
        }

        // If the user navigates via the drawer while a Welcome filter is open, clear that back stack
        // so the top-left button returns to the menu icon and back won't jump to a stale filter.
        clearContentBackStackIfNeeded();

        if (Category.ID_WELCOME.equals(category.getId())) {
            selectedCategoryId = null;
            navigateTo(getString(R.string.menu_welcome), true, true);
            return;
        }

        // Navigate by ID to avoid title collisions (e.g., a user category named "Welcome").
        selectedCategoryId = category.getId();
        selectedMenuTitle = normalizeTitle(category.getTitle());
        updateHeader(selectedMenuTitle);
        applyScreenChrome(selectedMenuTitle);
        updateDockSelection(selectedMenuTitle);
        showCategoryScreen(selectedMenuTitle, selectedCategoryId);

        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        refreshCategories();
    }

    private void onCategoryLongPressed(@NonNull View anchorView, @NonNull Category category) {
        if (Category.ID_WELCOME.equals(category.getId())) {
            return;
        }
        showCategoryActionsPopup(anchorView, category);
    }

    @NonNull
    private Category createWelcomeDrawerCategory() {
        // Virtual category: not stored in Firestore.
        return new Category(Category.ID_WELCOME, "🏠", getString(R.string.menu_welcome), 0);
    }

    private void showCategoryScreen(@NonNull String title, @Nullable String categoryId) {
        Fragment fragment = CategoryFragment.newInstance(title, categoryId);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFragmentContainer, fragment)
                .commit();
    }

    private void showCategoryActionsPopup(@NonNull View anchorView, @NonNull Category category) {
        PopupMenu popupMenu = new PopupMenu(this, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.menu_category_actions, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> handleCategoryAction(item, category));
        popupMenu.show();
    }

    private boolean handleCategoryAction(@NonNull MenuItem item, @NonNull Category category) {
        int id = item.getItemId();
        if (id == R.id.action_category_edit) {
            openEditCategoryScreen(category);
            return true;
        }
        if (id == R.id.action_category_delete) {
            confirmDeleteCategory(category);
            return true;
        }
        return false;
    }

    private void openEditCategoryScreen(@NonNull Category category) {
        if (isAddCategorySheetShowing()) {
            return;
        }
        updateFabVisibility(true);
        AddCategoryFragment sheet = AddCategoryFragment.newInstanceForEdit(
                category.getId(),
                category.getTitle(),
                category.getIcon()
        );
        sheet.show(getSupportFragmentManager(), TAG_ADD_CATEGORY_SHEET);
    }

    private void confirmDeleteCategory(@NonNull Category category) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_category_title)
                .setMessage(getString(R.string.delete_category_message, category.getTitle()))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> deleteCategory(category))
                .show();
    }

    private void deleteCategory(@NonNull Category category) {
        if (categoryRepository == null) {
            categoryRepository = new CategoryRepository();
        }
        String categoryId = category.getId();
        if (categoryId == null || categoryId.trim().isEmpty()) {
            Toast.makeText(this, R.string.delete_category_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        deleteTasksForCategory(categoryId, new FirestoreVoidCallback() {
            @Override
            public void onSuccess() {
                categoryRepository.deleteCategory(categoryId, new CategoryRepositoryContract.OnCategoryDeletedListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, R.string.delete_category_success, Toast.LENGTH_SHORT).show();
                        boolean wasSelected = categoryId.equals(selectedCategoryId);
                        if (wasSelected) {
                            selectedCategoryId = null;
                            navigateTo(getString(R.string.menu_welcome), true, true);
                        } else {
                            refreshCategories();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(MainActivity.this, R.string.delete_category_failed, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, R.string.delete_category_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private interface FirestoreVoidCallback {
        void onSuccess();

        void onError(@NonNull Exception e);
    }

    private void deleteTasksForCategory(@NonNull String categoryId, @NonNull FirestoreVoidCallback callback) {
        CollectionReference tasksRef = UserFirestorePaths.getUserCollection("tasks");
        if (tasksRef == null) {
            callback.onSuccess();
            return;
        }

        tasksRef.whereEqualTo("categoryId", categoryId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<DocumentSnapshot> documents = snapshot.getDocuments();
                    if (documents.isEmpty()) {
                        callback.onSuccess();
                        return;
                    }
                    deleteTasksInBatches(documents, 0, callback);
                })
                .addOnFailureListener(error -> callback.onError(asException(error)));
    }

    private void deleteTasksInBatches(@NonNull List<DocumentSnapshot> documents,
                                     int startIndex,
                                     @NonNull FirestoreVoidCallback callback) {
        if (startIndex >= documents.size()) {
            callback.onSuccess();
            return;
        }

        int endIndexExclusive = Math.min(startIndex + 450, documents.size());
        WriteBatch batch = FirebaseFirestore.getInstance().batch();
        for (int i = startIndex; i < endIndexExclusive; i++) {
            batch.delete(documents.get(i).getReference());
        }

        batch.commit()
                .addOnSuccessListener(unused -> deleteTasksInBatches(documents, endIndexExclusive, callback))
                .addOnFailureListener(error -> callback.onError(asException(error)));
    }

    @NonNull
    private Exception asException(@NonNull Exception e) {
        return e;
    }

    @NonNull
    private Exception asException(@NonNull Throwable throwable) {
        return throwable instanceof Exception ? (Exception) throwable : new Exception(throwable);
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
        clearContentBackStackIfNeeded();
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

    /**
     * Navigate to a Category screen by Firestore categoryId.
     * Used by Search results to jump directly to the category that contains a task.
     */
    public void openCategoryById(@NonNull String categoryId) {
        openCategoryById(categoryId, null);
    }

    /** Same as {@link #openCategoryById(String)} but optionally scrolls/highlights a specific task. */
    public void openCategoryById(@NonNull String categoryId, @Nullable String highlightTaskId) {
        if (categoryId.trim().isEmpty()) {
            return;
        }

        // Prefer using the existing menu-based navigation so header/dock/chrome stay consistent.
        Category found = null;
        for (Category c : menuCategories) {
            if (c != null && c.getId() != null && c.getId().trim().equals(categoryId.trim())) {
                found = c;
                break;
            }
        }

        clearContentBackStackIfNeeded();

        if (found != null && found.getTitle() != null && !found.getTitle().trim().isEmpty()) {
            selectedMenuTitle = normalizeTitle(found.getTitle());
            selectedCategoryId = categoryId.trim();
        } else {
            // Fallback if categories aren't loaded yet or title can't be resolved.
            selectedMenuTitle = getString(R.string.menu_categories);
            selectedCategoryId = categoryId.trim();
        }

        updateHeader(selectedMenuTitle);
        applyScreenChrome(selectedMenuTitle);
        updateDockSelection(selectedMenuTitle);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.contentFragmentContainer,
                        CategoryFragment.newInstance(selectedMenuTitle, selectedCategoryId, highlightTaskId)
                )
                .commit();
    }

    private void clearContentBackStackIfNeeded() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.isStateSaved()) {
            return;
        }
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
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

    /**
     * Allows child fragments (e.g., Welcome dashboard filters) to update the header title and chrome
     * without having to go through the drawer menu selection logic.
     */
    public void setScreenTitle(@NonNull String title) {
        selectedMenuTitle = normalizeTitle(title);
        // Filters are not tied to a single category.
        selectedCategoryId = null;
        updateHeader(selectedMenuTitle);
        applyScreenChrome(selectedMenuTitle);
        updateDockSelection(selectedMenuTitle);
    }

    /**
     * Opens a fragment on top of the current stack and updates the header title accordingly.
     * This is used by Welcome dashboard cards.
     */
    public void openFragmentWithTitle(@NonNull Fragment fragment, @NonNull String title) {
        setScreenTitle(title);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }
}