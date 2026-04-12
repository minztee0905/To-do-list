package com.example.ticktok.fragment;

import android.content.Context;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ticktok.R;
import com.example.ticktok.repository.CategoryRepository;

public class AddCategoryFragment extends Fragment {

    private static final String DEFAULT_ICON = "≡";
    public static final String RESULT_KEY_ADD_CATEGORY = "result_add_category";
    public static final String RESULT_KEY_CATEGORY_NAME = "result_category_name";
    public static final String RESULT_KEY_CATEGORY_ICON = "result_category_icon";

    private ImageView iv_close;
    private TextView tv_save;
    private EditText et_category_icon;
    private EditText et_category_name;
    private CategoryRepository categoryRepository;
    private boolean isSaving;
    private final InputFilter iconOnlyFilter = (source, start, end, dest, dstart, dend) -> {
        if (source == null || start == end) {
            return null;
        }

        SpannableStringBuilder filtered = new SpannableStringBuilder();
        boolean changed = false;

        for (int index = start; index < end; ) {
            int codePoint = Character.codePointAt(source, index);
            int charCount = Character.charCount(codePoint);

            if (isAllowedIconCodePoint(codePoint)) {
                filtered.append(source, index, index + charCount);
            } else {
                changed = true;
            }

            index += charCount;
        }

        return changed ? filtered : null;
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        categoryRepository = new CategoryRepository();
        setupListeners();
    }

    private void initViews(@NonNull View view) {
        iv_close = view.findViewById(R.id.iv_close);
        tv_save = view.findViewById(R.id.tv_save);
        et_category_icon = view.findViewById(R.id.et_category_icon);
        et_category_name = view.findViewById(R.id.et_category_name);
    }

    private void setupListeners() {
        if (et_category_icon != null) {
            appendIconOnlyFilter();

            et_category_icon.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    et_category_icon.post(() -> et_category_icon.selectAll());
                    showKeyboard(et_category_icon);
                }
            });

            et_category_icon.setOnClickListener(v -> {
                showKeyboard(et_category_icon);
                et_category_icon.post(() -> et_category_icon.selectAll());
            });
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
        } else if (!isValidIconInput(icon)) {
            Toast.makeText(requireContext(), "Icon chỉ cho phép emoji hoặc ký hiệu", Toast.LENGTH_SHORT).show();
            return;
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

        categoryRepository.addCategory(name, icon, new CategoryRepository.OnCategorySavedListener() {
            @Override
            public void onSuccess(String message) {
                if (!isAdded()) {
                    return;
                }

                Bundle result = new Bundle();
                result.putString(RESULT_KEY_CATEGORY_NAME, name);
                result.putString(RESULT_KEY_CATEGORY_ICON, icon);
                getParentFragmentManager().setFragmentResult(RESULT_KEY_ADD_CATEGORY, result);

                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
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

    private void appendIconOnlyFilter() {
        if (et_category_icon == null) {
            return;
        }

        InputFilter[] existing = et_category_icon.getFilters();
        InputFilter[] merged = new InputFilter[existing.length + 1];
        System.arraycopy(existing, 0, merged, 0, existing.length);
        merged[existing.length] = iconOnlyFilter;
        et_category_icon.setFilters(merged);
    }

    private boolean isValidIconInput(@NonNull String value) {
        boolean hasVisibleSymbol = false;
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            if (!isAllowedIconCodePoint(codePoint)) {
                return false;
            }
            if (codePoint != 0x200D && codePoint != 0xFE0F) {
                hasVisibleSymbol = true;
            }
            i += Character.charCount(codePoint);
        }
        return hasVisibleSymbol;
    }

    private boolean isAllowedIconCodePoint(int codePoint) {
        if (Character.isLetterOrDigit(codePoint) || Character.isWhitespace(codePoint)) {
            return false;
        }

        if (codePoint == 0x200D || codePoint == 0xFE0F) {
            return true;
        }

        if (codePoint >= 0x1F3FB && codePoint <= 0x1F3FF) {
            return true;
        }

        if ((codePoint >= 0x1F000 && codePoint <= 0x1FAFF)
                || (codePoint >= 0x2600 && codePoint <= 0x27BF)) {
            return true;
        }

        int type = Character.getType(codePoint);
        return type == Character.OTHER_SYMBOL
                || type == Character.MATH_SYMBOL
                || type == Character.CURRENCY_SYMBOL
                || type == Character.MODIFIER_SYMBOL
                || type == Character.OTHER_PUNCTUATION;
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
        if (isAdded()) {
            getParentFragmentManager().popBackStack();
        }
    }
}





