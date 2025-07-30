package com.cremcash.provisionalreciept;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.cremcash.provisionalreciept.databinding.ActivityReceiptBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReceiptActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityReceiptBinding binding;

    public String uid;


    TextView receiptNumber;
    TextInputEditText payorEditText, amountPaidEditText, formOfPaymentEditText,
            totalAmountEditText, receivedByEditText, dateReceivedEditText;
    MaterialButton btnSubmit;

    private String potentialNextReceiptNumber = null;
    private String branchForPotentialReceiptNumber = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReceiptBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up Material Toolbar
        setSupportActionBar(binding.toolbar);

        // Reference all input views
        //receiptNumber = findViewById(R.id.receiptNumber);
        payorEditText = findViewById(R.id.payorEditText);
        amountPaidEditText = findViewById(R.id.amountPaidEditText);
        formOfPaymentEditText = findViewById(R.id.formOfPaymentEditText);
        totalAmountEditText = findViewById(R.id.totalAmountEditText);
        receivedByEditText = findViewById(R.id.receivedByEditText);
        dateReceivedEditText = findViewById(R.id.dateReceivedEditText);
        btnSubmit = findViewById(R.id.btnSubmit);

        SessionManager session = new SessionManager(getApplicationContext());
        uid = session.getUserId();

        receivedByEditText.setText(uid);
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        String currentDate = sdf.format(new Date());
        dateReceivedEditText.setText(currentDate);

        if (getIntent().getBooleanExtra("from_excel", false)) {
            binding.payorEditText.setText(getIntent().getStringExtra("payor"));
            binding.amountPaidEditText.setText(getIntent().getStringExtra("amountInWords"));
            binding.formOfPaymentEditText.setText(getIntent().getStringExtra("formOfPayment"));
            binding.totalAmountEditText.setText(formatWithCommasAndDecimals(
                    String.valueOf(getIntent().getDoubleExtra("totalAmount", 0))));
            binding.receivedByEditText.setText(getIntent().getStringExtra("receivedBy"));
            binding.dateReceivedEditText.setText(getIntent().getStringExtra("dateReceived"));

            // 🌐 Try generating a new receipt number if online
            executor.execute(() -> {
                if (canConnectToServer()) {
                    if (uid == null || uid.isEmpty()) {
                        Log.e("ReceiptActivity", "Employee ID not passed to activity!");
                        Toast.makeText(this, "Error: Employee ID missing.", Toast.LENGTH_LONG).show();
                        // Potentially finish the activity or handle this error appropriately
                        finish();
                        return;
                    }
                    loadNextPotentialReceiptNumber();
                } else {
                    handler.post(() -> Toast.makeText(this, "Offline: Using existing Receipt No.", Toast.LENGTH_SHORT).show());
                }
            });

        } else {
            if (uid == null || uid.isEmpty()) {
                Log.e("ReceiptActivity", "Employee ID not passed to activity!");
                Toast.makeText(this, "Error: Employee ID missing.", Toast.LENGTH_LONG).show();
                // Potentially finish the activity or handle this error appropriately
                finish();
                return;
            }
            // 🔢 Default case: generate fresh receipt number
            loadNextPotentialReceiptNumber();
        }

        // Button event
//        btnSubmit.setOnClickListener(v -> {
//            if (validateForm()) {
//                new UploadTask().execute();
//            }
//        });

        btnSubmit.setOnClickListener(v -> {
            if (validateForm()) {
                // Pass the currently displayed receipt number (which should be potentialNextReceiptNumber)
                // and the branch to the UploadTask.
                // This ensures that the task uses the number that was validated and is about to be submitted.
                String receiptNumberToSubmit = binding.receiptNumber.getText().toString();

                if (branchForPotentialReceiptNumber != null &&
                        potentialNextReceiptNumber != null &&
                        receiptNumberToSubmit.equals(potentialNextReceiptNumber)) {

                    new UploadTask(receiptNumberToSubmit, branchForPotentialReceiptNumber).execute();

                } else {
                    // This indicates a mismatch or that potential numbers weren't loaded correctly.
                    Log.e("SubmitButton", "Mismatch or missing potential receipt number/branch. Aborting submission.");
                    Log.e("SubmitButton", "Branch: " + branchForPotentialReceiptNumber + ", Potential: " + potentialNextReceiptNumber + ", Current UI: " + receiptNumberToSubmit);
                    Toast.makeText(ReceiptActivity.this, "Error with receipt number. Please try reloading.", Toast.LENGTH_LONG).show();
                    // Optionally, reload the potential number:
                    // loadNextPotentialReceiptNumber();
                }
            }
        });

        totalAmountEditText.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    totalAmountEditText.removeTextChangedListener(this);

                    String cleanString = s.toString().replaceAll(",", "");
                    if (!cleanString.isEmpty() && !cleanString.equals(".")) {
                        try {
                            double parsed = Double.parseDouble(cleanString);

                            // Remember cursor position
                            int cursorPosition = totalAmountEditText.getSelectionStart();
                            int initialLength = s.length();

                            // Format number manually
                            java.text.NumberFormat nf = new java.text.DecimalFormat("#,##0.##");
                            String[] parts = cleanString.split("\\.");
                            String formatted = nf.format(Double.parseDouble(parts[0]));
                            if (parts.length > 1) {
                                formatted += "." + parts[1];
                            } else if (cleanString.contains(".")) {
                                formatted += ".";
                            }

                            current = formatted;
                            totalAmountEditText.setText(formatted);

                            // Calculate updated cursor position
                            int newLength = formatted.length();
                            int selection = cursorPosition + (newLength - initialLength);
                            if (selection > 0 && selection <= formatted.length()) {
                                totalAmountEditText.setSelection(selection);
                            } else {
                                totalAmountEditText.setSelection(formatted.length());
                            }

                            // Update amount in words only when valid
                            String amountInWords = convertAmountToWords(parsed);
                            amountPaidEditText.setText(amountInWords);

                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }

                    totalAmountEditText.addTextChangedListener(this);
                }
            }


        });




        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }

    private String formatWithCommasAndDecimals(String input) {
        try {
            String clean = input.replaceAll(",", "");
            double parsed = Double.parseDouble(clean);
            java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###.00");
            return formatter.format(parsed);
        } catch (Exception e) {
            return input;
        }
    }

//    @SuppressLint("StaticFieldLeak")
//    private class UploadTask extends AsyncTask<Void, Void, Boolean> {
//        private ProgressDialog progressDialog;
//        private AlertDialog materialDialog;
//
//
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//
//            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(ReceiptActivity.this)
//                    .setTitle("Uploading receipt")
//                    .setMessage("Uploading receipt. If offline, it will be saved locally...")
//                    .setCancelable(false)
//                    .setView(R.layout.dialog_loading); // Custom layout with ProgressBar
//
//            materialDialog = builder.create();
//            materialDialog.show();
//        }
//
//
//        @Override
//        protected Boolean doInBackground(Void... voids) {
//            if (!canConnectToServer()) {
//                Log.w("UploadTask", "Offline: Cannot connect to server.");
//                return null; // signal offline mode
//            }
//
//            try {
//                Class.forName("net.sourceforge.jtds.jdbc.Driver");
//                Connection conn = DriverManager.getConnection(ConnectionClass.dbUrl, ConnectionClass.dbUser, ConnectionClass.dbPassword);
//
//                String receiptNo = binding.receiptNumber.getText().toString();
//                String payor = binding.payorEditText.getText().toString();
//                String amountInWords = binding.amountPaidEditText.getText().toString();
//                String formOfPayment = binding.formOfPaymentEditText.getText().toString();
//                String totalAmountStr = binding.totalAmountEditText.getText().toString().replaceAll(",", "");
//                String receivedBy = binding.receivedByEditText.getText().toString();
//                String dateReceived = binding.dateReceivedEditText.getText().toString();
//
//                double totalAmount = Double.parseDouble(totalAmountStr);
//
//                String sql = "INSERT INTO ProvisionalReceipts " +
//                        "(ReceiptNo, Payor, AmountInWords, FormOfPayment, TotalAmount, ReceivedBy, DateReceived) " +
//                        "VALUES (?, ?, ?, ?, ?, ?, ?)";
//                PreparedStatement stmt = conn.prepareStatement(sql);
//                stmt.setString(1, receiptNo);
//                stmt.setString(2, payor);
//                stmt.setString(3, amountInWords);
//                stmt.setString(4, formOfPayment);
//                stmt.setDouble(5, totalAmount);
//                stmt.setString(6, receivedBy);
//                stmt.setString(7, dateReceived);
//
//                int result = stmt.executeUpdate();
//
//                stmt.close();
//                conn.close();
//
//                return result > 0;
//            } catch (Exception e) {
//                Log.e("UploadTask", "DB insert failed", e);
//                return false;
//            }
//        }
//
//
//        @Override
//        protected void onPostExecute(Boolean success) {
//            if (materialDialog != null && materialDialog.isShowing()) {
//                materialDialog.dismiss();
//            }
//
//            if (success != null && success) {
//                Toast.makeText(ReceiptActivity.this, "✅ Uploaded to server", Toast.LENGTH_SHORT).show();
//
//                // If launched from Excel list
//                if (getIntent().getBooleanExtra("from_excel", false)) {
//
//                    // 🔐 Capture filename BEFORE clearing inputs
//                    String payorName = binding.payorEditText.getText().toString()
//                            .trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
//                    if (payorName.isEmpty()) payorName = "Unnamed";
//                    File file = new File(getExternalFilesDir(null), "OfflineReceipts/" + payorName + "_receipts.xlsx");
//
//                    clearInputs();
//
//                    loadNextPotentialReceiptNumber();
//
//                    // Show loading dialog while deleting
//                    AlertDialog deleteDialog = new MaterialAlertDialogBuilder(ReceiptActivity.this)
//                            .setTitle("Cleaning up")
//                            .setMessage("Deleting local Excel file...")
//                            .setCancelable(false)
//                            .setView(R.layout.dialog_loading)
//                            .create();
//                    deleteDialog.show();
//
//                    // Perform deletion
//                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                        Log.d("UploadTask", "Attempting to delete file: " + file.getAbsolutePath());
//
//                        if (file.exists()) {
//                            if (file.delete()) {
//                                Toast.makeText(ReceiptActivity.this, "🗑️ Offline Excel deleted", Toast.LENGTH_SHORT).show();
//                            } else {
//                                Toast.makeText(ReceiptActivity.this, "⚠️ Failed to delete Excel file", Toast.LENGTH_LONG).show();
//                                Log.w("UploadTask", "Failed to delete: " + file.getAbsolutePath());
//                            }
//                        } else {
//                            Log.w("UploadTask", "Excel file does not exist: " + file.getAbsolutePath());
//                        }
//
//                        deleteDialog.dismiss();
//
//                        setResult(RESULT_OK);
//
//                        Bundle result = new Bundle();
//                        result.putBoolean("refresh", true);
//
//                        getSupportFragmentManager().setFragmentResult("refresh_db_list", result);
//
//                        finish();
//                    }, 600);
//
//                } else {
//                    clearInputs();
//                    loadNextPotentialReceiptNumber();
//                    setResult(RESULT_OK);
//
//                    Bundle result = new Bundle();
//                    result.putBoolean("refresh", true);
//
//                    getSupportFragmentManager().setFragmentResult("refresh_db_list", result);
//
//                    finish();
//                }
//
//
//            } else if (success == null) {
//                if (materialDialog != null) {
//                    materialDialog.setMessage("Offline mode: saving locally...");
//                }
//
//                new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                    saveReceiptToExcel();
//                    clearInputs();
//
//                    if (materialDialog != null && materialDialog.isShowing()) {
//                        materialDialog.dismiss();
//                    }
//
//                    Toast.makeText(ReceiptActivity.this, "📁 Saved offline", Toast.LENGTH_SHORT).show();
//
//                    NavController nav = Navigation.findNavController(
//                            ReceiptActivity.this,
//                            R.id.nav_host_fragment_activity_main
//                    );
//                    nav.navigate(R.id.navigation_dashboard);
//
//                }, 500);
//            } else {
//                Toast.makeText(ReceiptActivity.this, "❌ Upload failed. Try again.", Toast.LENGTH_LONG).show();
//            }
//        }
//
//    }

    @SuppressLint("StaticFieldLeak")
    private class UploadTask extends AsyncTask<Void, Void, Boolean> {
        private AlertDialog materialDialog;

        // Store the receipt number and branch that are being submitted
        private String taskReceiptNo;
        private String taskBranch;
        private boolean isOfflineSave = false; // Flag to indicate if it was an offline save

        // Constructor to receive the values
        public UploadTask(String receiptNo, String branch) {
            this.taskReceiptNo = receiptNo;
            this.taskBranch = branch;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(ReceiptActivity.this)
                    .setTitle("Processing Receipt") // Changed title slightly
                    .setMessage("Submitting receipt details...")
                    .setCancelable(false)
                    .setView(R.layout.dialog_loading);
            materialDialog = builder.create();
            materialDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (!canConnectToServer()) {
                Log.w("UploadTask", "Offline: Cannot connect to server. Preparing for local save.");
                isOfflineSave = true;
                // For offline, we don't try to insert into the server's ProvisionalReceipts table here.
                // The actual saving to Excel will happen in onPostExecute.
                // We return 'null' to indicate this special offline state.
                return null;
            }

            isOfflineSave = false; // Explicitly set to false if online
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                Connection conn = DriverManager.getConnection(ConnectionClass.dbUrl, ConnectionClass.dbUser, ConnectionClass.dbPassword);

                // Use taskReceiptNo and taskBranch passed via constructor
                String payor = Objects.requireNonNull(binding.payorEditText.getText()).toString();
                String amountInWords = Objects.requireNonNull(binding.amountPaidEditText.getText()).toString();
                String formOfPayment = Objects.requireNonNull(binding.formOfPaymentEditText.getText()).toString();
                String totalAmountStr = Objects.requireNonNull(binding.totalAmountEditText.getText()).toString().replaceAll(",", "");
                String receivedBy = Objects.requireNonNull(binding.receivedByEditText.getText()).toString(); // This is likely the UID/EmployeeID
                String dateReceived = Objects.requireNonNull(binding.dateReceivedEditText.getText()).toString();

                double totalAmount = Double.parseDouble(totalAmountStr);

                // IMPORTANT: Add 'Branch' column to your ProvisionalReceipts table if it doesn't exist
                String sql = "INSERT INTO ProvisionalReceipts " +
                        "(ReceiptNo, Payor, AmountInWords, FormOfPayment, TotalAmount, ReceivedBy, DateReceived, Branch) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, taskReceiptNo);       // Use the task's receipt number
                stmt.setString(2, payor);
                stmt.setString(3, amountInWords);
                stmt.setString(4, formOfPayment);
                stmt.setDouble(5, totalAmount);
                stmt.setString(6, receivedBy);
                stmt.setString(7, dateReceived);
                stmt.setString(8, taskBranch);          // Use the task's branch

                int result = stmt.executeUpdate();
                stmt.close();
                conn.close();
                return result > 0; // True if insert into ProvisionalReceipts was successful

            } catch (Exception e) {
                Log.e("UploadTask", "DB insert into ProvisionalReceipts failed", e);
                return false; // False if there was an error during online upload
            }
        }

        @Override
        protected void onPostExecute(Boolean onlineUploadSuccess) {
            if (materialDialog != null && materialDialog.isShowing()) {
                materialDialog.dismiss();
            }

            if (isOfflineSave) { // Check the flag first
                // Handle offline saving to Excel
                // This is the equivalent of your 'success == null' block
                Log.d("UploadTask", "Processing offline save.");
                saveReceiptToExcel(); // You need to ensure this method correctly uses form data
                clearInputs();
                Toast.makeText(ReceiptActivity.this, "📁 Saved offline. Sequence not updated on server.", Toast.LENGTH_LONG).show();

                // DO NOT finalize sequence for offline saves
                // DO NOT try to delete Excel if it was just saved offline

                // Navigate as per your original logic for offline
                NavController nav = Navigation.findNavController(
                        ReceiptActivity.this,
                        R.id.nav_host_fragment_activity_main
                );
                if (nav.getCurrentDestination() != null && nav.getCurrentDestination().getId() != R.id.navigation_dashboard) {
                    nav.navigate(R.id.navigation_dashboard);
                }
                // If already on dashboard, maybe just refresh it or do nothing extra for navigation
                // finish(); // Or finish if that's the desired behavior after offline save.

            } else if (onlineUploadSuccess != null && onlineUploadSuccess) {
                // Online upload was successful
                Toast.makeText(ReceiptActivity.this, "✅ Uploaded to server.", Toast.LENGTH_SHORT).show();

                // **** KEY CHANGE: Finalize the sequence number on the server ****
                if (taskReceiptNo != null && taskBranch != null) {
                    Log.d("UploadTask", "Finalizing sequence for Branch: " + taskBranch + ", ReceiptNo: " + taskReceiptNo);
                    finalizeReceiptSequence(taskBranch, taskReceiptNo);
                } else {
                    Log.e("UploadTask", "Critical error: taskReceiptNo or taskBranch is null. Cannot finalize sequence.");
                    // This shouldn't happen if the call to UploadTask constructor is correct.
                    Toast.makeText(ReceiptActivity.this, "Error: Could not finalize sequence (null data).", Toast.LENGTH_LONG).show();
                }

                // --- Handle the "from_excel" case AFTER successful online upload & sequence finalization ---
                if (getIntent().getBooleanExtra("from_excel", false)) {
                    String payorName = Objects.requireNonNull(binding.payorEditText.getText()).toString()
                            .trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
                    if (payorName.isEmpty()) payorName = "Unnamed";
                    File file = new File(getExternalFilesDir(null), "OfflineReceipts/" + payorName + "_receipts.xlsx");

                    // Clear inputs BEFORE showing delete dialog, but AFTER data is used
                    clearInputs();
                    loadNextPotentialReceiptNumber(); // Load next number for the UI

                    AlertDialog deleteDialog = new MaterialAlertDialogBuilder(ReceiptActivity.this)
                            .setTitle("Cleaning up")
                            .setMessage("Deleting local Excel file (as it's now on server)...")
                            .setCancelable(false)
                            .setView(R.layout.dialog_loading)
                            .create();
                    deleteDialog.show();

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Log.d("UploadTask", "Attempting to delete Excel file: " + file.getAbsolutePath());
                        if (file.exists()) {
                            if (file.delete()) {
                                Toast.makeText(ReceiptActivity.this, "🗑️ Offline Excel deleted.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ReceiptActivity.this, "⚠️ Failed to delete Excel file.", Toast.LENGTH_LONG).show();
                                Log.w("UploadTask", "Failed to delete: " + file.getAbsolutePath());
                            }
                        } else {
                            Log.w("UploadTask", "Excel file for deletion does not exist: " + file.getAbsolutePath());
                        }
                        deleteDialog.dismiss();
                        setResult(RESULT_OK);
                        Bundle resultBundle = new Bundle(); // Renamed to avoid conflict
                        resultBundle.putBoolean("refresh", true);
                        getSupportFragmentManager().setFragmentResult("refresh_db_list", resultBundle);
                        finish();
                    }, 600);
                } else {
                    // Regular online success (not from Excel)
                    clearInputs();
                    loadNextPotentialReceiptNumber(); // Load next for UI
                    setResult(RESULT_OK);
                    Bundle resultBundle = new Bundle(); // Renamed
                    resultBundle.putBoolean("refresh", true);
                    getSupportFragmentManager().setFragmentResult("refresh_db_list", resultBundle);
                    // finish(); // Decide if you want to finish or stay for another receipt
                }
            } else {
                // Online upload failed (onlineUploadSuccess is false)
                Toast.makeText(ReceiptActivity.this, "❌ Upload failed. Please try again. Sequence not updated.", Toast.LENGTH_LONG).show();
                // DO NOT finalize sequence
                // DO NOT clear inputs, so user can retry
            }
        }
    }

    private boolean validateForm() {
        if (binding.payorEditText.getText().toString().trim().isEmpty()) {
            binding.payorEditText.setError("Payor is required");
            return false;
        }

        String amountStr = binding.totalAmountEditText.getText().toString().replace(",", "");
        if (amountStr.isEmpty()) {
            binding.totalAmountEditText.setError("Total amount is required");
            return false;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                binding.totalAmountEditText.setError("Amount must be greater than zero");
                return false;
            }
        } catch (NumberFormatException e) {
            binding.totalAmountEditText.setError("Invalid amount format");
            return false;
        }

        return true;
    }


    private boolean canConnectToServer() {
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            DriverManager.setLoginTimeout(5); // Optional: 5-second timeout
            Connection conn = DriverManager.getConnection(ConnectionClass.dbUrl, ConnectionClass.dbUser, ConnectionClass.dbPassword);
            conn.close();
            return true;
        } catch (Exception e) {
            Log.e("ConnectivityCheck", "Server unreachable", e);
            return false;
        }
    }


    private void clearInputs() {
        binding.payorEditText.setText("");
        binding.amountPaidEditText.setText("");
        binding.formOfPaymentEditText.setText("");
        binding.totalAmountEditText.setText("");
        // Do not clear these if they’re auto-filled:
        // binding.receivedByEditText.setText("");
        // binding.dateReceivedEditText.setText("");
    }


    private void saveReceiptToExcel() {
        try {
            File dir = new File(getExternalFilesDir(null), "OfflineReceipts");
            if (!dir.exists()) dir.mkdirs();

            // Build filename from payor
            String payorName = binding.payorEditText.getText().toString().trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
            if (payorName.isEmpty()) payorName = "Unnamed";
            String fileName = payorName + "_receipts.xlsx";

            File file = new File(dir, fileName);

            Workbook workbook;
            Sheet sheet;

            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                workbook = new XSSFWorkbook(fis);
                sheet = workbook.getSheetAt(0);
                fis.close();
            } else {
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet("Receipts");

                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("ReceiptNo");
                header.createCell(1).setCellValue("Payor");
                header.createCell(2).setCellValue("AmountInWords");
                header.createCell(3).setCellValue("FormOfPayment");
                header.createCell(4).setCellValue("TotalAmount");
                header.createCell(5).setCellValue("ReceivedBy");
                header.createCell(6).setCellValue("DateReceived");
            }

            int lastRow = sheet.getLastRowNum() + 1;
            Row row = sheet.createRow(lastRow);

            row.createCell(0).setCellValue(binding.receiptNumber.getText().toString());
            row.createCell(1).setCellValue(binding.payorEditText.getText().toString());
            row.createCell(2).setCellValue(binding.amountPaidEditText.getText().toString());
            row.createCell(3).setCellValue(binding.formOfPaymentEditText.getText().toString());
            row.createCell(4).setCellValue(binding.totalAmountEditText.getText().toString().replaceAll(",", ""));
            row.createCell(5).setCellValue(binding.receivedByEditText.getText().toString());
            row.createCell(6).setCellValue(binding.dateReceivedEditText.getText().toString());

            FileOutputStream fos = new FileOutputStream(file);
            workbook.write(fos);
            workbook.close();
            fos.close();

            Toast.makeText(this, "Saved offline to " + fileName, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save offline receipt", Toast.LENGTH_LONG).show();
        }
    }



    private String convertAmountToWords(double amount) {
        if (amount == 0) return "Zero Pesos";

        long pesos = (long) amount;
        int centavos = (int) Math.round((amount - pesos) * 100);

        return numberToWords(pesos) + " Pesos" + (centavos > 0 ? " and " + centavos + "/100" : "");
    }

    private String numberToWords(long number) {
        if (number == 0) return "Zero";

        String[] units = {
                "", "One", "Two", "Three", "Four", "Five",
                "Six", "Seven", "Eight", "Nine", "Ten", "Eleven",
                "Twelve", "Thirteen", "Fourteen", "Fifteen",
                "Sixteen", "Seventeen", "Eighteen", "Nineteen"
        };

        String[] tens = {
                "", "", "Twenty", "Thirty", "Forty", "Fifty",
                "Sixty", "Seventy", "Eighty", "Ninety"
        };

        StringBuilder words = new StringBuilder();

        if (number >= 1_000_000) {
            words.append(numberToWords(number / 1_000_000)).append(" Million ");
            number %= 1_000_000;
        }

        if (number >= 1000) {
            words.append(numberToWords(number / 1000)).append(" Thousand ");
            number %= 1000;
        }

        if (number >= 100) {
            words.append(units[(int)(number / 100)]).append(" Hundred ");
            number %= 100;
        }

        if (number >= 20) {
            words.append(tens[(int)(number / 10)]).append(" ");
            number %= 10;
        }

        if (number > 0) {
            words.append(units[(int) number]).append(" ");
        }

        return words.toString().trim();
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

//    private void loadNextReceiptNumber() {
//        executor.execute(() -> {
//            String nextNumber = "1";
//            try {
//                Class.forName("net.sourceforge.jtds.jdbc.Driver");
//                Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
//                String query = "SELECT MAX(CAST(ReceiptNo AS INT)) AS MaxReceiptNo FROM ProvisionalReceipts";
//                PreparedStatement stmt = conn.prepareStatement(query);
//                java.sql.ResultSet rs = stmt.executeQuery();
//
//                if (rs.next()) {
//                    int maxReceipt = rs.getInt("MaxReceiptNo");
//                    nextNumber = String.valueOf(maxReceipt + 1);
//                }
//
//                rs.close();
//                stmt.close();
//                conn.close();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            // Update UI on main thread
//            String finalNextNumber = nextNumber;
//            handler.post(() -> {
//                Log.d("ReceiptNumber", "Loaded value: " + finalNextNumber);
//                binding.receiptNumber.setText(finalNextNumber);
//            });
//        });
//    }

    private String fetchBranchForEmployee(String empId) {
        String branch = null;
        Connection hrisConn = null;
        PreparedStatement hrisStmt = null;
        java.sql.ResultSet hrisRs = null;

        if (empId == null || empId.isEmpty()) {
            Log.e("FetchBranch", "Employee ID is null or empty.");
            return null;
        }

        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            hrisConn = DriverManager.getConnection(ConnectionClass.hrisDbUrl, ConnectionClass.hrisDbUser, ConnectionClass.hrisDbPassword);
            String query = "SELECT Branch FROM tblHRDEmployees WHERE Employee_ID = ?";
            hrisStmt = hrisConn.prepareStatement(query);
            hrisStmt.setString(1, empId);
            hrisRs = hrisStmt.executeQuery();

            if (hrisRs.next()) {
                branch = hrisRs.getString("Branch");
                if (branch != null) {
                    branch = branch.trim(); // Clean up whitespace
                }
            } else {
                Log.w("FetchBranch", "No branch found for Employee_ID: " + empId);
            }
        } catch (Exception e) {
            Log.e("FetchBranch", "Error fetching branch for Employee_ID: " + empId, e);
        } finally {
            try {
                if (hrisRs != null) hrisRs.close();
                if (hrisStmt != null) hrisStmt.close();
                if (hrisConn != null) hrisConn.close();
            } catch (Exception e) {
                Log.e("FetchBranch", "Error closing HRIS DB resources", e);
            }
        }
        return branch;
    }
    private void loadNextPotentialReceiptNumber() {
        executor.execute(() -> {
            final String currentBranch = fetchBranchForEmployee(uid); // uid should be your employeeId

            if (currentBranch == null || currentBranch.isEmpty()) {
                Log.e("ReceiptNumber", "Could not determine branch for employee: " + uid + ". Cannot fetch potential receipt number.");
                handler.post(() -> {
                    Toast.makeText(ReceiptActivity.this, "Error: Could not determine user branch for receipt no.", Toast.LENGTH_LONG).show();
                    binding.receiptNumber.setText("Error");
                    potentialNextReceiptNumber = null;
                    branchForPotentialReceiptNumber = null;
                });
                return;
            }

            String nextNumberToDisplay = "1"; // Default
            Connection conn = null;

            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                conn = DriverManager.getConnection(ConnectionClass.dbUrl, ConnectionClass.dbUser, ConnectionClass.dbPassword);
                // No transaction needed here as we are only reading

                String selectQuery = "SELECT ReceiptNo FROM PRSeq WHERE Branch = ?";
                PreparedStatement selectStmt = conn.prepareStatement(selectQuery);
                selectStmt.setString(1, currentBranch);
                java.sql.ResultSet rs = selectStmt.executeQuery();

                int currentSeqNumberInDB = 0;
                if (rs.next()) {
                    try {
                        currentSeqNumberInDB = Integer.parseInt(rs.getString("ReceiptNo"));
                    } catch (NumberFormatException e) {
                        Log.e("ReceiptNumber", "Invalid number format in PRSeq.ReceiptNo for branch " + currentBranch, e);
                    }
                } else {
                    Log.w("ReceiptNumber", "No PRSeq entry for Branch: " + currentBranch + ". Next potential number will be 1.");
                }
                rs.close();
                selectStmt.close();
                nextNumberToDisplay = String.valueOf(currentSeqNumberInDB + 1);

            } catch (Exception e) {
                Log.e("ReceiptNumber", "Error fetching potential receipt number for branch " + currentBranch, e);
                // Keep nextNumberToDisplay as "1" or handle error display
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (Exception e) {
                        Log.e("ReceiptNumber", "Error closing DB connection during potential number fetch.", e);
                    }
                }
            }

            final String finalNextNumberToDisplay = nextNumberToDisplay;
            handler.post(() -> {
                binding.receiptNumber.setText(finalNextNumberToDisplay);
                potentialNextReceiptNumber = finalNextNumberToDisplay; // Store for submission
                branchForPotentialReceiptNumber = currentBranch;      // Store for submission
                Log.d("ReceiptNumber", "Potential next number for branch " + currentBranch + " is " + finalNextNumberToDisplay);
            });
        });
    }

    /**
     * Updates the PRSeq table with the used receipt number for the given branch.
     * This should be called ONLY after the main receipt data has been successfully saved.
     *
     * @param branch The branch for which the sequence is being updated.
     * @param usedReceiptNumber The actual receipt number that was used and saved.
     */
    private void finalizeReceiptSequence(String branch, String usedReceiptNumber) {
        executor.execute(() -> {
            Connection conn = null;
            boolean updateSuccess = false;
            boolean rowExisted;

            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                conn = DriverManager.getConnection(ConnectionClass.dbUrl, ConnectionClass.dbUser, ConnectionClass.dbPassword);
                conn.setAutoCommit(false);

                // Check if branch row exists to decide between INSERT and UPDATE
                String checkQuery = "SELECT ReceiptNo FROM PRSeq WHERE Branch = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
                checkStmt.setString(1, branch);
                java.sql.ResultSet rs = checkStmt.executeQuery();
                rowExisted = rs.next();
                rs.close();
                checkStmt.close();

                PreparedStatement updateOrInsertStmt;
                if (rowExisted) {
                    String updateQuery = "UPDATE PRSeq SET ReceiptNo = ? WHERE Branch = ?";
                    updateOrInsertStmt = conn.prepareStatement(updateQuery);
                    updateOrInsertStmt.setString(1, usedReceiptNumber);
                    updateOrInsertStmt.setString(2, branch);
                } else {
                    String insertQuery = "INSERT INTO PRSeq (Branch, ReceiptNo) VALUES (?, ?)";
                    updateOrInsertStmt = conn.prepareStatement(insertQuery);
                    updateOrInsertStmt.setString(1, branch);
                    updateOrInsertStmt.setString(2, usedReceiptNumber);
                }

                int rowsAffected = updateOrInsertStmt.executeUpdate();
                updateOrInsertStmt.close();

                if (rowsAffected > 0) {
                    conn.commit();
                    updateSuccess = true;
                } else {
                    conn.rollback();
                    Log.e("ReceiptSequenceFinalize", "Failed to update/insert PRSeq for branch: " + branch + ". Rows affected: " + rowsAffected);
                }
            } catch (Exception e) {
                Log.e("ReceiptSequenceFinalize", "Error finalizing receipt sequence for branch " + branch, e);
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (Exception ex) {
                        Log.e("ReceiptSequenceFinalize", "Error rolling back transaction.", ex);
                    }
                }
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (Exception e) {
                        Log.e("ReceiptSequenceFinalize", "Error closing DB connection.", e);
                    }
                }
            }

            final boolean finalUpdateSuccess = updateSuccess;
            handler.post(() -> {
                if (finalUpdateSuccess) {
                    Log.d("ReceiptSequenceFinalize", "Successfully updated PRSeq for branch " + branch + " to " + usedReceiptNumber);
                } else {
                    // This is a critical error if the main receipt saved but this failed.
                    // Consider how to handle this (e.g., manual reconciliation flag).
                    Toast.makeText(ReceiptActivity.this, "CRITICAL: Receipt saved, but FAILED to update sequence number for branch " + branch + "!", Toast.LENGTH_LONG).show();
                }
            });
        });
    }



    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Close the activity when back button is pressed
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
