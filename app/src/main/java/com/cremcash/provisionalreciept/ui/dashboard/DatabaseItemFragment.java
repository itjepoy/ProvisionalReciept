package com.cremcash.provisionalreciept.ui.dashboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cremcash.provisionalreciept.R;
import com.cremcash.provisionalreciept.ReceiptItem;
import com.cremcash.provisionalreciept.DashboardAdapter;
import com.cremcash.provisionalreciept.ReceiptViewerActivity;
import com.cremcash.provisionalreciept.SessionManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import androidx.appcompat.widget.SearchView;

public class DatabaseItemFragment extends Fragment {

    private RecyclerView recyclerView;
    private DashboardAdapter adapter;
    private ArrayList<ReceiptItem> receiptList = new ArrayList<>();

    // Database connection info
    private final String dbUrl = "jdbc:jtds:sqlserver://10.10.0.73:1433/CAMFIN";
    private final String dbUser = "sa";
    private final String dbPassword = "g@t3k33p3R2024";

    private SwipeRefreshLayout swipeRefreshLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_item_list, container, false);

        recyclerView = view.findViewById(R.id.recycler_dashboard);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new DashboardAdapter(receiptList, item -> {
            Intent intent = new Intent(getContext(), ReceiptViewerActivity.class);
            intent.putExtra("receiptNo", item.getReceiptNo());
            intent.putExtra("payor", item.getPayor());
            intent.putExtra("date", item.getDateReceived());
            intent.putExtra("amount", item.getAmountInWords());
            intent.putExtra("total", item.getTotalAmount());
            intent.putExtra("receivedBy", item.getReceivedBy());
            intent.putExtra("form", item.getFormOfPayment());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        new LoadReceiptsTask().execute();

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Trigger reload on pull
            new LoadReceiptsTask().execute();
        });

        SearchView searchView = view.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });


        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            // ✅ Refresh your database list here
            new LoadReceiptsTask().execute();  // or however you load your DB list
        }
    }


    @SuppressLint("StaticFieldLeak")
    private class LoadReceiptsTask extends AsyncTask<Void, Void, ArrayList<ReceiptItem>> {

        SessionManager session = new SessionManager(requireContext());
        String uid = session.getUserId();


        @Override
        protected ArrayList<ReceiptItem> doInBackground(Void... voids) {
            ArrayList<ReceiptItem> results = new ArrayList<>();
            Connection conn = null;
            Statement stmt = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;

            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

                String sql = "SELECT * FROM ProvisionalReceipts WHERE ReceivedBy = ?";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, uid); // safely bind uid parameter

                rs = pstmt.executeQuery();

                while (rs.next()) {
                    results.add(new ReceiptItem(
                            rs.getString("ReceiptNo"),
                            rs.getString("Payor"),
                            rs.getString("AmountInWords"),
                            rs.getString("FormOfPayment"),
                            rs.getDouble("TotalAmount"),
                            rs.getString("ReceivedBy"),
                            rs.getString("DateReceived")
                    ));
                }

            } catch (Exception e) {
                Log.e("DB_ERROR", "Error loading receipts", e);
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (pstmt != null) pstmt.close(); // use pstmt instead of stmt
                    if (conn != null) conn.close();
                } catch (Exception e) {
                    Log.e("DB_CLOSE_ERROR", "Error closing DB resources", e);
                }
            }
            return results;
        }

        @Override
        protected void onPostExecute(ArrayList<ReceiptItem> result) {
            adapter.updateData(result);  // ✅ this updates both fullList and displayList correctly
            swipeRefreshLayout.setRefreshing(false);

            Log.d("DashboardFragment", "Loaded " + result.size() + " receipts");
            Toast.makeText(getContext(), "Loaded " + result.size() + " receipts", Toast.LENGTH_SHORT).show();
        }

    }
}
