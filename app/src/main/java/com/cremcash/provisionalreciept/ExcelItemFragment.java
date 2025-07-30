package com.cremcash.provisionalreciept;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import com.cremcash.provisionalreciept.ui.dashboard.DatabaseItemFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.appcompat.widget.SearchView;

public class ExcelItemFragment extends Fragment {

    private RecyclerView recyclerView;
    private DashboardAdapter adapter;
    private final ArrayList<ReceiptItem> excelList = new ArrayList<>();

    private SwipeRefreshLayout swipeRefreshLayoutExcel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_item_list, container, false);

        recyclerView = view.findViewById(R.id.recycler_dashboard);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DashboardAdapter(excelList, item -> {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {
                boolean canConnect = ServerChecker.canConnectToServer(
                        "jdbc:jtds:sqlserver://10.10.0.73:1433/CAMFIN", "sa", "g@t3k33p3R2024"
                );

                handler.post(() -> {
                    if (canConnect) {
                        Intent intent = getIntent(item);
                        startActivityForResult(intent, 1001);
                    } else {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Server Unreachable")
                                .setMessage("SQL Server is not reachable. Please connect to the server before proceeding.")
                                .setPositiveButton("OK", null)
                                .show();
                    }
                });
            });
        });


        recyclerView.setAdapter(adapter);

        swipeRefreshLayoutExcel = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayoutExcel.setOnRefreshListener(this::loadExcelReceipts);

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

    @NonNull
    private Intent getIntent(ReceiptItem item) {
        Intent intent = new Intent(getContext(), ReceiptActivity.class);
        intent.putExtra("from_excel", true);
        intent.putExtra("payor", item.getPayor());
        intent.putExtra("amountInWords", item.getAmountInWords());
        intent.putExtra("formOfPayment", item.getFormOfPayment());
        intent.putExtra("totalAmount", item.getTotalAmount());
        intent.putExtra("receivedBy", item.getReceivedBy());
        intent.putExtra("dateReceived", item.getDateReceived());
        return intent;
    }

    public static class ServerChecker {
        public static boolean canConnectToServer(String dbUrl, String dbUser, String dbPassword) {
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                DriverManager.setLoginTimeout(3); // short timeout
                Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                conn.close();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }


    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }



    @Override
    public void onResume() {
        super.onResume();
        loadExcelReceipts();
    }

    private void loadExcelReceipts() {
        excelList.clear(); // Clear previous data to avoid duplicates

        try {
            File dir = new File(requireContext().getExternalFilesDir(null), "OfflineReceipts");

            if (dir.exists() && dir.isDirectory()) {
                File[] excelFiles = dir.listFiles((d, name) -> name.endsWith(".xlsx"));

                if (excelFiles != null && excelFiles.length > 0) {
                    for (File file : excelFiles) {
                        try (FileInputStream fis = new FileInputStream(file);
                             Workbook workbook = new XSSFWorkbook(fis)) {

                            Sheet sheet = workbook.getSheetAt(0);

                            for (Row row : sheet) {
                                if (row.getRowNum() == 0 || row == null || row.getPhysicalNumberOfCells() < 7)
                                    continue;

                                try {
                                    String receiptNo = getSafeString(row, 0);
                                    String payor = getSafeString(row, 1);
                                    String amountInWords = getSafeString(row, 2);
                                    String form = getSafeString(row, 3);
                                    double total = getSafeDouble(row, 4);
                                    String receivedBy = getSafeString(row, 5);
                                    String date = getSafeString(row, 6);

                                    excelList.add(new ReceiptItem(receiptNo, payor, amountInWords, form, total, receivedBy, date));
                                } catch (Exception rowErr) {
                                    rowErr.printStackTrace();
                                }
                            }

                        } catch (Exception fileErr) {
                            fileErr.printStackTrace();
                        }
                    }
                } else {
                    Log.d("ExcelLoad", "No offline Excel files found.");
                }

                //adapter.notifyDataSetChanged(); // ✅ Notify after all files processed
                adapter.updateData(excelList);  // custom method to reset internal list and refresh display
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error reading offline receipts", Toast.LENGTH_LONG).show();
        } finally {
            // If using SwipeRefreshLayout, stop animation here
            if (swipeRefreshLayoutExcel != null && swipeRefreshLayoutExcel.isRefreshing()) {
                swipeRefreshLayoutExcel.setRefreshing(false);
            }
        }
    }



    private String getSafeString(Row row, int index) {
        try {
            return row.getCell(index) != null ? row.getCell(index).getStringCellValue() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private double getSafeDouble(Row row, int index) {
        try {
            if (row.getCell(index) == null) return 0.0;
            switch (row.getCell(index).getCellType()) {
                case NUMERIC:
                    return row.getCell(index).getNumericCellValue();
                case STRING:
                    return Double.parseDouble(row.getCell(index).getStringCellValue().replace(",", "").trim());
                default:
                    return 0.0;
            }
        } catch (Exception e) {
            return 0.0;
        }
    }



}
