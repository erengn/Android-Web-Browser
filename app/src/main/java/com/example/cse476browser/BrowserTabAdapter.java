package com.example.cse476browser;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class BrowserTabAdapter extends RecyclerView.Adapter<BrowserTabAdapter.TabViewHolder> {

    public interface OnTabClickListener {
        void onTabClick(int position);

        void onTabClose(int position);
    }

    private final ArrayList<BrowserTab> tabs;
    private final OnTabClickListener listener;
    private int selectedPosition = 0;

    public BrowserTabAdapter(ArrayList<BrowserTab> tabs, OnTabClickListener listener) {
        this.tabs = tabs;
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_browser_tab, parent, false);
        return new TabViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
        BrowserTab tab = tabs.get(position);

        String title = tab.getTitle();
        if (title == null || title.isEmpty()) {
            title = holder.itemView.getContext().getString(R.string.new_tab_title);
        }
        holder.title.setText(title);

        if (tab.getFavicon() != null) {
            holder.favicon.setImageBitmap(tab.getFavicon());
        } else {
            holder.favicon.setImageResource(R.drawable.ic_default_favicon);
        }

        boolean selected = position == selectedPosition;
        holder.itemView.setSelected(selected);
        holder.indicator.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                listener.onTabClick(pos);
            }
        });
        holder.closeButton.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                listener.onTabClose(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tabs.size();
    }

    static class TabViewHolder extends RecyclerView.ViewHolder {
        ImageView favicon;
        TextView title;
        ImageButton closeButton;
        View indicator;

        TabViewHolder(@NonNull View itemView) {
            super(itemView);
            favicon = itemView.findViewById(R.id.tabFavicon);
            title = itemView.findViewById(R.id.tabTitle);
            closeButton = itemView.findViewById(R.id.tabCloseButton);
            indicator = itemView.findViewById(R.id.tabActiveIndicator);
        }
    }
}
