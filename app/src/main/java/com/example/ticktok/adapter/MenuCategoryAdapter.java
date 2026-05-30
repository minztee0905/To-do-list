package com.example.ticktok.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticktok.R;
import com.example.ticktok.model.Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MenuCategoryAdapter extends RecyclerView.Adapter<MenuCategoryAdapter.CategoryViewHolder> {

    private boolean isWelcomeCategory(@Nullable Category category) {
        return category != null && Category.ID_WELCOME.equals(category.getId());
    }

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public interface OnCategoryLongClickListener {
        void onCategoryLongClick(@NonNull View anchorView, @NonNull Category category);
    }

    public interface OnStartDragListener {
        void onStartDrag(@NonNull RecyclerView.ViewHolder viewHolder);
    }

    private final List<Category> categories = new ArrayList<>();
    private final OnCategoryClickListener clickListener;
    private final OnCategoryLongClickListener longClickListener;
    private final OnStartDragListener dragListener;
    private String selectedTitle = "";
    @Nullable
    private String selectedCategoryId;

    public MenuCategoryAdapter(@NonNull OnCategoryClickListener clickListener,
                               @Nullable OnCategoryLongClickListener longClickListener,
                               @Nullable OnStartDragListener dragListener) {
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
        this.dragListener = dragListener;
    }

    public void submitList(List<Category> items, String selectedTitle) {
        submitList(items, selectedTitle, null);
    }

    public void submitList(List<Category> items, String selectedTitle, @Nullable String selectedCategoryId) {
        categories.clear();
        categories.addAll(items);
        this.selectedTitle = selectedTitle == null ? "" : selectedTitle;
        this.selectedCategoryId = selectedCategoryId;
        notifyDataSetChanged();
    }

    public void moveItem(int fromPosition, int toPosition) {

        if (fromPosition == 0 || toPosition == 0) {
            return;
        }
        if (fromPosition < 0 || fromPosition >= categories.size()
                || toPosition < 0 || toPosition >= categories.size()) {
            return;
        }
        if (fromPosition == toPosition) {
            return;
        }
        Collections.swap(categories, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    @NonNull
    public List<Category> getCurrentItems() {
        return new ArrayList<>(categories);
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_menu_category, parent, false);
        return new CategoryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        holder.bind(categories.get(position));
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtIcon;
        private final TextView txtTitle;
        private final ImageView ivDragHandle;
        private final View root;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView;
            txtIcon = itemView.findViewById(R.id.txtCategoryIcon);
            txtTitle = itemView.findViewById(R.id.txtCategoryTitle);
            ivDragHandle = itemView.findViewById(R.id.ivDragHandle);
        }

        void bind(Category category) {
            txtIcon.setText(category.getIcon());
            txtTitle.setText(category.getTitle());

            boolean isWelcome = isWelcomeCategory(category);

            boolean isSelected;
            if (selectedCategoryId != null && !selectedCategoryId.trim().isEmpty()) {
                isSelected = category.getId() != null && category.getId().equals(selectedCategoryId);
            } else {
                isSelected = category.getTitle() != null && category.getTitle().equalsIgnoreCase(selectedTitle);
            }
            if (isSelected) {
                root.setBackgroundResource(R.drawable.home_menu_item_bg);
                root.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(root.getContext(), R.color.menu_highlight)
                ));
            } else {
                root.setBackgroundResource(0);
                root.setBackgroundTintList(null);
            }

            root.setOnClickListener(v -> clickListener.onCategoryClick(category));


            root.setOnLongClickListener(v -> {
                if (isWelcome) {
                    return false;
                }
                if (dragListener == null) {
                    return false;
                }
                dragListener.onStartDrag(CategoryViewHolder.this);
                return true;
            });


            if (ivDragHandle != null) {
                ivDragHandle.setVisibility(isWelcome ? View.INVISIBLE : View.VISIBLE);
                ivDragHandle.setOnClickListener(v -> {
                    if (isWelcome) {
                        return;
                    }
                    if (longClickListener != null) {
                        longClickListener.onCategoryLongClick(v, category);
                    }
                });
            }
        }
    }
}

