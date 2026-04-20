package com.example.ticktok.fragment;

import android.app.DatePickerDialog;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.ticktok.R;
import com.example.ticktok.model.Event;
import com.example.ticktok.util.UserFirestorePaths;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddEventBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String DEFAULT_EVENT_ICON = "●";

    private ImageView ivClose;
    private TextView tvSave;
    private EditText etEventIcon;
    private EditText etEventName;
    private ImageButton btnEventCalendar;
    private TextView tvEventDateValue;
    private View iconPickerContainer;
    private GridView gvEventIcons;

    private String selectedEventIcon = DEFAULT_EVENT_ICON;
    private long selectedTargetDate = 0L;
    private boolean isSaving;
    private IconGridAdapter iconGridAdapter;

    private final IconOption[] iconOptions = new IconOption[] {
            new IconOption("●", "Mặc định"),
            new IconOption("📌", "Ghim"),
            new IconOption("🎉", "Tiệc"),
            new IconOption("🎂", "Sinh nhật"),
            new IconOption("🔥", "Quan trọng"),
            new IconOption("⭐", "Ưu tiên"),
            new IconOption("🏆", "Mục tiêu"),
            new IconOption("🧳", "Du lịch")
    };

    public AddEventBottomSheetFragment() {
    }

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
        return inflater.inflate(R.layout.bottom_sheet_add_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupListeners();
        updateSelectedDateLabel();
        updateSaveEnabledState();

        if (etEventName != null) {
            etEventName.requestFocus();
            etEventName.post(() -> showKeyboard(etEventName));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ivClose = null;
        tvSave = null;
        etEventIcon = null;
        etEventName = null;
        btnEventCalendar = null;
        tvEventDateValue = null;
        iconPickerContainer = null;
        gvEventIcons = null;
        iconGridAdapter = null;
    }

    private void initViews(@NonNull View view) {
        ivClose = view.findViewById(R.id.iv_close);
        tvSave = view.findViewById(R.id.tv_save);
        etEventIcon = view.findViewById(R.id.et_event_icon);
        etEventName = view.findViewById(R.id.et_event_name);
        btnEventCalendar = view.findViewById(R.id.btnEventCalendar);
        tvEventDateValue = view.findViewById(R.id.tvEventDateValue);
        iconPickerContainer = view.findViewById(R.id.iconPickerContainer);
        gvEventIcons = view.findViewById(R.id.gvEventIcons);

        if (etEventIcon != null) {
            etEventIcon.setText(DEFAULT_EVENT_ICON);
        }
    }

    private void setupListeners() {
        if (ivClose != null) {
            ivClose.setOnClickListener(v -> dismissAllowingStateLoss());
        }
        if (tvSave != null) {
            tvSave.setOnClickListener(v -> handleSaveAction());
        }
        if (btnEventCalendar != null) {
            btnEventCalendar.setOnClickListener(v -> showDatePicker());
        }
        if (etEventIcon != null) {
            setupIconPickerOnlyMode();
            etEventIcon.setOnClickListener(v -> toggleIconPicker());
        }
        if (etEventName != null) {
            etEventName.setOnClickListener(v -> setIconPickerVisible(false));
        }

        if (gvEventIcons != null) {
            iconGridAdapter = new IconGridAdapter();
            gvEventIcons.setAdapter(iconGridAdapter);
            gvEventIcons.setOnItemClickListener((parent, view, position, id) -> {
                selectedEventIcon = iconOptions[position].icon;
                if (etEventIcon != null) {
                    etEventIcon.setText(selectedEventIcon);
                }
                if (iconGridAdapter != null) {
                    iconGridAdapter.notifyDataSetChanged();
                }
                setIconPickerVisible(false);
                if (etEventName != null) {
                    etEventName.requestFocus();
                    showKeyboard(etEventName);
                }
            });
        }
    }

    private void handleSaveAction() {
        if (isSaving || etEventName == null || etEventIcon == null) {
            return;
        }

        if (selectedTargetDate <= 0) {
            return;
        }

        String title = etEventName.getText() != null ? etEventName.getText().toString().trim() : "";
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), R.string.add_event_name_hint, Toast.LENGTH_SHORT).show();
            return;
        }

        String iconInput = etEventIcon.getText() != null ? etEventIcon.getText().toString().trim() : "";
        selectedEventIcon = iconInput.isEmpty() ? DEFAULT_EVENT_ICON : iconInput;
        Long targetDate = selectedTargetDate > 0 ? selectedTargetDate : null;

        Event event = new Event(title, selectedEventIcon, targetDate);
        event.setCreatedAt(null);

        setSavingState(true);
        CollectionReference eventsRef = UserFirestorePaths.getUserCollection("events");
        if (eventsRef == null) {
            setSavingState(false);
            Toast.makeText(requireContext(), R.string.auth_error_login_required, Toast.LENGTH_SHORT).show();
            return;
        }

        eventsRef
                .add(event)
                .addOnSuccessListener(documentReference -> {
                    documentReference.update("createdAt", FieldValue.serverTimestamp());
                    if (!isAdded()) {
                        return;
                    }
                    Toast.makeText(requireContext(), R.string.add_event_success, Toast.LENGTH_SHORT).show();
                    hideKeyboard();
                    dismissAllowingStateLoss();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) {
                        return;
                    }
                    setSavingState(false);
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setSavingState(boolean saving) {
        isSaving = saving;
        updateSaveEnabledState();
    }

    private void updateSaveEnabledState() {
        if (tvSave == null) {
            return;
        }
        boolean isDateSelected = selectedTargetDate > 0;
        boolean enabled = isDateSelected && !isSaving;
        tvSave.setEnabled(enabled);
        tvSave.setAlpha(enabled ? 1f : 0.45f);
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, y, m, d) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(y, m, d, 0, 0, 0);
                    selectedCalendar.set(Calendar.MILLISECOND, 0);
                    selectedTargetDate = selectedCalendar.getTimeInMillis();
                    if (btnEventCalendar != null) {
                        btnEventCalendar.setColorFilter(Color.parseColor("#FF9800"));
                    }
                    updateSelectedDateLabel();
                    updateSaveEnabledState();
                },
                year,
                month,
                day
        );
        datePickerDialog.show();
    }

    private void updateSelectedDateLabel() {
        if (tvEventDateValue == null) {
            return;
        }
        if (selectedTargetDate <= 0) {
            tvEventDateValue.setText(R.string.add_event_date_empty);
            return;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String formatted = formatter.format(new Date(selectedTargetDate));
        tvEventDateValue.setText(getString(R.string.add_event_date_selected, formatted));
    }

    private void setupIconPickerOnlyMode() {
        if (etEventIcon == null) {
            return;
        }
        etEventIcon.setFocusable(false);
        etEventIcon.setFocusableInTouchMode(false);
        etEventIcon.setCursorVisible(false);
        etEventIcon.setLongClickable(false);
        etEventIcon.setTextIsSelectable(false);
        etEventIcon.setShowSoftInputOnFocus(false);
    }

    private void toggleIconPicker() {
        if (!isAdded()) {
            return;
        }
        if (etEventIcon != null && etEventIcon.getText() != null) {
            String value = etEventIcon.getText().toString().trim();
            selectedEventIcon = value.isEmpty() ? DEFAULT_EVENT_ICON : value;
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

    private void showKeyboard(@NonNull View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
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
            boolean isSelected = iconOptions[position].icon.equals(selectedEventIcon);
            bg.setColor(Color.parseColor(isSelected ? "#FF9800" : "#2A2A2A"));
            itemView.setTextColor(Color.parseColor(isSelected ? "#121212" : "#FFFFFF"));
            itemView.setBackground(bg);
            return itemView;
        }
    }
}
