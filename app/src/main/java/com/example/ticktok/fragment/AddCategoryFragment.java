package com.example.ticktok.fragment;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.ticktok.R;
import com.example.ticktok.activity.MainActivity;
import com.example.ticktok.repository.CategoryRepository;
import com.example.ticktok.repository.CategoryRepositoryContract;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class AddCategoryFragment extends BottomSheetDialogFragment {

    private static final String DEFAULT_ICON = "≡";
    public static final String RESULT_KEY_ADD_CATEGORY = "result_add_category";
    public static final String RESULT_KEY_CATEGORY_NAME = "result_category_name";
    public static final String RESULT_KEY_CATEGORY_ICON = "result_category_icon";

    private ImageView iv_close;
    private TextView tv_save;
    private EditText et_category_icon;
    private EditText et_category_name;
    private View iconPickerContainer;
    private GridView gvCategoryIcons;
    private IconGridAdapter iconGridAdapter;
    private String selectedIconValue = DEFAULT_ICON;
    private CategoryRepositoryContract categoryRepository;
    private boolean isSaving;
    private final IconOption[] iconOptions = new IconOption[] {
            new IconOption("≡", "Mặc định"),
            new IconOption("📁", "Chung"),
            new IconOption("💼", "Công việc"),
            new IconOption("🏠", "Gia đình"),
            new IconOption("🛒", "Mua sắm"),
            new IconOption("💰", "Tài chính"),
            new IconOption("📚", "Học tập"),
            new IconOption("💪", "Sức khỏe"),
            new IconOption("🎯", "Mục tiêu"),
            new IconOption("🧳", "Du lịch"),
            new IconOption("❤️", "Quan trọng"),
            new IconOption("🎉", "Sự kiện")
    };

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                            | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
            );
        }

        dialog.setOnShowListener(d -> {
            int bottomSheetId = getResources().getIdentifier("design_bottom_sheet", "id", "com.google.android.material");
            FrameLayout bottomSheet = dialog.findViewById(bottomSheetId);
            if (bottomSheet == null) {
                return;
            }

            bottomSheet.setBackgroundResource(android.R.color.transparent);
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        categoryRepository = new CategoryRepository();
        setupListeners();

        if (et_category_name != null) {
            et_category_name.requestFocus();
            et_category_name.post(() -> showKeyboard(et_category_name));
        }
    }

    private void initViews(@NonNull View view) {
        iv_close = view.findViewById(R.id.iv_close);
        tv_save = view.findViewById(R.id.tv_save);
        et_category_icon = view.findViewById(R.id.et_category_icon);
        et_category_name = view.findViewById(R.id.et_category_name);
        iconPickerContainer = view.findViewById(R.id.iconPickerContainer);
        gvCategoryIcons = view.findViewById(R.id.gvCategoryIcons);
    }

    private void setupListeners() {
        if (et_category_icon != null) {
            setupIconPickerOnlyMode();
            et_category_icon.setOnClickListener(v -> toggleIconPicker());
        }

        if (gvCategoryIcons != null) {
            iconGridAdapter = new IconGridAdapter();
            gvCategoryIcons.setAdapter(iconGridAdapter);
            gvCategoryIcons.setOnItemClickListener((parent, view, position, id) -> {
                IconOption selected = iconOptions[position];
                selectedIconValue = selected.icon;
                if (et_category_icon != null) {
                    et_category_icon.setText(selected.icon);
                }
                if (iconGridAdapter != null) {
                    iconGridAdapter.notifyDataSetChanged();
                }
                setIconPickerVisible(false);
                if (et_category_name != null) {
                    et_category_name.requestFocus();
                    showKeyboard(et_category_name);
                }
            });
        }

        if (et_category_name != null) {
            et_category_name.setOnClickListener(v -> setIconPickerVisible(false));
        }

        if (iv_close != null) {
            iv_close.setOnClickListener(v -> handleCancelAction());
        }

        if (tv_save != null) {
            tv_save.setOnClickListener(v -> handleSaveAction());
        }
    }

    private void handleCancelAction() {
        hideKeyboard();
        closeScreen();
    }

    private void handleSaveAction() {
        if (isSaving) {
            return;
        }

        if (et_category_name == null || et_category_icon == null) {
            return;
        }

        String name = et_category_name.getText() != null
                ? et_category_name.getText().toString().trim()
                : "";

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show();
            return;
        }

        String icon = et_category_icon.getText() != null
                ? et_category_icon.getText().toString().trim()
                : "";

        if (icon.isEmpty()) {
            icon = DEFAULT_ICON;
        }

        setSavingState(true);
        saveCategoryToDatabase(name, icon);
    }

    private void saveCategoryToDatabase(@NonNull String name, @NonNull String icon) {
        if (categoryRepository == null) {
            setSavingState(false);
            Toast.makeText(requireContext(), R.string.error_load_categories, Toast.LENGTH_SHORT).show();
            return;
        }

        categoryRepository.addCategory(name, icon, new CategoryRepositoryContract.OnCategorySavedListener() {
            @Override
            public void onSuccess() {
                if (!isAdded()) {
                    return;
                }

                Bundle result = new Bundle();
                result.putString(RESULT_KEY_CATEGORY_NAME, name);
                result.putString(RESULT_KEY_CATEGORY_ICON, icon);
                getParentFragmentManager().setFragmentResult(RESULT_KEY_ADD_CATEGORY, result);

                Toast.makeText(requireContext(), R.string.add_category_success, Toast.LENGTH_SHORT).show();
                hideKeyboard();
                closeScreen();
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) {
                    return;
                }
                setSavingState(false);
                Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setSavingState(boolean saving) {
        isSaving = saving;
        if (tv_save != null) {
            tv_save.setEnabled(!saving);
            tv_save.setAlpha(saving ? 0.6f : 1f);
        }
    }

    private void setupIconPickerOnlyMode() {
        if (et_category_icon == null) {
            return;
        }
        et_category_icon.setFocusable(false);
        et_category_icon.setFocusableInTouchMode(false);
        et_category_icon.setCursorVisible(false);
        et_category_icon.setLongClickable(false);
        et_category_icon.setTextIsSelectable(false);
        et_category_icon.setShowSoftInputOnFocus(false);
    }

    private void toggleIconPicker() {
        if (!isAdded()) {
            return;
        }
        if (et_category_icon != null && et_category_icon.getText() != null) {
            String value = et_category_icon.getText().toString().trim();
            selectedIconValue = value.isEmpty() ? DEFAULT_ICON : value;
        }
        if (iconGridAdapter != null) {
            iconGridAdapter.notifyDataSetChanged();
        }
        setIconPickerVisible(iconPickerContainer == null || iconPickerContainer.getVisibility() != View.VISIBLE);
    }

    private void setIconPickerVisible(boolean visible) {
        if (iconPickerContainer != null) {
            iconPickerContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private void hideKeyboard() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) {
            return;
        }

        View target = null;
        if (getActivity() != null) {
            target = getActivity().getCurrentFocus();
        }
        if (target == null) {
            target = getView();
        }

        if (target != null) {
            imm.hideSoftInputFromWindow(target.getWindowToken(), 0);
        }
    }
    private void showKeyboard(View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }
    private void closeScreen() {
        dismissAllowingStateLoss();
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).onAddCategorySheetDismissed();
        }
    }

    private static class IconOption {
        final String icon;
        final String label;

        IconOption(String icon, String label) {
            this.icon = icon;
            this.label = label;
        }
    }

    private class IconGridAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return iconOptions.length;
        }

        @Override
        public Object getItem(int position) {
            return iconOptions[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView itemView = convertView instanceof TextView
                    ? (TextView) convertView
                    : new TextView(parent.getContext());

            int size = dpToPx(56);
            itemView.setLayoutParams(new GridView.LayoutParams(size, size));
            itemView.setGravity(Gravity.CENTER);
            itemView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
            itemView.setText(iconOptions[position].icon);
            itemView.setContentDescription(iconOptions[position].label);
            itemView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_white));

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dpToPx(14));
            boolean isSelected = iconOptions[position].icon.equals(selectedIconValue);
            bg.setColor(Color.parseColor(isSelected ? "#FF9800" : "#2A2A2A"));
            itemView.setTextColor(Color.parseColor(isSelected ? "#121212" : "#FFFFFF"));
            itemView.setBackground(bg);
            return itemView;
        }
    }
}

