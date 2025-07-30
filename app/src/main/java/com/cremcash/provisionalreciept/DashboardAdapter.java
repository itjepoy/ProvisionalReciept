package com.cremcash.provisionalreciept;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.ViewHolder> {

    private final List<ReceiptItem> fullList;
    private final List<ReceiptItem> receiptList;
    private final OnItemClickListener listener;

    // Click listener interface
    public interface OnItemClickListener {
        void onItemClick(ReceiptItem item);
    }

    // Adapter constructor
    public DashboardAdapter(ArrayList<ReceiptItem> receiptList, OnItemClickListener listener) {
        this.listener = listener;
        this.fullList = new ArrayList<>(receiptList);
        this.receiptList = new ArrayList<>(receiptList);

    }

    // ViewHolder class
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView receiptNoText, payorText, formOfPaymentText, totalAmountText, dateReceivedText;

        ViewHolder(View itemView) {
            super(itemView);
            receiptNoText = itemView.findViewById(R.id.receipt_no_text);
            payorText = itemView.findViewById(R.id.payor_text);
            formOfPaymentText = itemView.findViewById(R.id.form_of_payment_text);
            totalAmountText = itemView.findViewById(R.id.total_amount_text);
            dateReceivedText = itemView.findViewById(R.id.date_received_text);
        }

        // Bind data and click listener to item view
        public void bind(final ReceiptItem item, final OnItemClickListener listener) {
            itemView.setOnClickListener(v -> {
                listener.onItemClick(item);
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_receipt, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReceiptItem item = receiptList.get(position);

        holder.receiptNoText.setText(item.getReceiptNo());
        holder.payorText.setText(item.getPayor());
        holder.formOfPaymentText.setText(item.getFormOfPayment());

        String formattedAmount = NumberFormat.getCurrencyInstance(new Locale("en", "PH"))
                .format(item.getTotalAmount());
        holder.totalAmountText.setText(formattedAmount);

        holder.dateReceivedText.setText(item.getDateReceived());

        // Bind click event
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return receiptList.size();
    }

    public void filter(String query) {
        receiptList.clear();
        if (query == null || query.trim().isEmpty()) {
            receiptList.addAll(fullList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (ReceiptItem item : fullList) {
                if (item.getPayor().toLowerCase().contains(lowerCaseQuery) ||
                        item.getDateReceived().toLowerCase().contains(lowerCaseQuery) ||
                        item.getReceiptNo().toLowerCase().contains(lowerCaseQuery)) {
                    receiptList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void updateData(List<ReceiptItem> newData) {
        fullList.clear();
        fullList.addAll(newData);
        filter(""); // reset filter
    }
}
