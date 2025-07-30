package com.cremcash.provisionalreciept.ui;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.cremcash.provisionalreciept.R;
import com.cremcash.provisionalreciept.ReceiptItem;

import java.util.List;

public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.DashboardViewHolder> {

    private List<ReceiptItem> itemList;

    public DashboardAdapter(List<ReceiptItem> itemList) {
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public DashboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard, parent, false);
        return new DashboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DashboardViewHolder holder, int position) {
        ReceiptItem item = itemList.get(position);
        holder.textItem.setText(item.toString());
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class DashboardViewHolder extends RecyclerView.ViewHolder {
        TextView textItem;

        public DashboardViewHolder(@NonNull View itemView) {
            super(itemView);
            textItem = itemView.findViewById(R.id.textItem);
        }
    }
}