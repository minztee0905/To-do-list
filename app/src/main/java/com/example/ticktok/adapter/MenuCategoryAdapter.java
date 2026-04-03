package com.example.ticktok.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticktok.R;
import com.example.ticktok.model.Category;

import java.util.ArrayList;
import java.util.List;

public class MenuCategoryAdapter extends RecyclerView.Adapter<MenuCategoryAdapter.CategoryViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    private final List<Category> categories = new ArrayList<>();
    private final OnCategoryClickListener clickListener;
    private String selectedTitle = "";

    public MenuCategoryAdapter(OnCategoryClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void submitList(List<Category> items, String selectedTitle) {
        categories.clear();
        categories.addAll(items);
        this.selectedTitle = selectedTitle == null ? "" : selectedTitle;
        notifyDataSetChanged();
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
        private final View root;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView;
            txtIcon = itemView.findViewById(R.id.txtCategoryIcon);
            txtTitle = itemView.findViewById(R.id.txtCategoryTitle);
        }

        void bind(Category category) {
            txtIcon.setText(category.getIcon());
            txtTitle.setText(category.getTitle());

            boolean isSelected = category.getTitle().equalsIgnoreCase(selectedTitle);
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
        }
    }
}

