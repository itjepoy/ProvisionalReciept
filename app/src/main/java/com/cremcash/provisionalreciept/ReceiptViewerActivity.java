package com.cremcash.provisionalreciept;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

public class ReceiptViewerActivity extends AppCompatActivity {

    private FloatingActionButton fabPrintReceipt;
    private CardView receiptCardView;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_viewer);

        TextView tvReceiptNo = findViewById(R.id.tv_receipt_no_value);
        TextView tvPayor = findViewById(R.id.tv_payor_value);
        TextView tvAmount = findViewById(R.id.tv_amount_value);
        TextView tvForm = findViewById(R.id.tv_form_value);
        TextView tvReceivedBy = findViewById(R.id.tv_received_by_value);
        TextView tvDate = findViewById(R.id.tv_date_received_value);
        TextView tvTotal = findViewById(R.id.tv_total_value);

        fabPrintReceipt = findViewById(R.id.fab_print_receipt);
        receiptCardView = findViewById(R.id.receipt_card);

        // Get data from intent
        String receiptNo = getIntent().getStringExtra("receiptNo");
        String payor = getIntent().getStringExtra("payor");
        String date = getIntent().getStringExtra("date");
        String amount = getIntent().getStringExtra("amount");
        String form = getIntent().getStringExtra("form");
        String receivedBy = getIntent().getStringExtra("receivedBy");
        double total = getIntent().getDoubleExtra("total", 0.0);

        // Display the receipt details
        tvReceiptNo.setText(receiptNo);
        tvPayor.setText(payor);
        tvAmount.setText(amount);
        tvForm.setText(form);
        tvReceivedBy.setText(receivedBy);
        tvDate.setText(date);
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH")); // ₱ format
        tvTotal.setText(currencyFormat.format(total));


        fabPrintReceipt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                printReceipt();
            }
        });
    }

    @OptIn(markerClass = UnstableApi.class)
    private void printReceipt() {
        if (receiptCardView == null) {
            androidx.media3.common.util.Log.e("PrintReceipt", "Receipt CardView is null. Cannot print.");
            return;
        }

        PrintManager printManager = (PrintManager) this.getSystemService(Context.PRINT_SERVICE);
        String jobName = getString(R.string.app_name) + " Document"; // Make sure to have app_name in strings.xml

        // Pass the CardView to the adaptercan I connect i
        ViewPrintAdapter printAdapter = new ViewPrintAdapter(this, receiptCardView, jobName);

        if (printManager != null) {
            printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());
        } else {
            androidx.media3.common.util.Log.e("PrintReceipt", "PrintManager is null. Cannot print.");
        }
    }

    public static class ViewPrintAdapter extends PrintDocumentAdapter {
        private final Context context;
        private final View viewToPrint;
        private final String jobName;
        private int pageHeight;
        private int pageWidth;
        private PdfDocument myPdfDocument;
        private int totalpages = 1; // Assuming 1 page for the CardView bitmap

        public ViewPrintAdapter(Context context, View view, String jobName) {
            this.context = context;
            this.viewToPrint = view;
            this.jobName = jobName;
        }

        @Override
        public void onLayout(PrintAttributes oldAttributes,
                             PrintAttributes newAttributes,
                             CancellationSignal cancellationSignal,
                             LayoutResultCallback callback,
                             Bundle extras) {

            myPdfDocument = new PdfDocument(); // Create new instance for each print job

            pageHeight = newAttributes.getMediaSize().getHeightMils() * 72 / 1000; // Standard PDF points (1/72 inch)
            pageWidth = newAttributes.getMediaSize().getWidthMils() * 72 / 1000;


            if (cancellationSignal.isCanceled()) {
                callback.onLayoutCancelled();
                return;
            }

            // For this example, we assume the content fits on one page.
            // For more complex content, you would calculate totalpages here.
            // totalpages = computePageCount(newAttributes);


            if (totalpages > 0) {
                PrintDocumentInfo.Builder builder = new PrintDocumentInfo
                        .Builder(jobName) // Use the jobName passed to the constructor
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT) // Or CONTENT_TYPE_PHOTO
                        .setPageCount(totalpages);

                PrintDocumentInfo info = builder.build();
                callback.onLayoutFinished(info, !oldAttributes.equals(newAttributes));
            } else {
                callback.onLayoutFailed("Page count calculation failed.");
            }
        }


        @OptIn(markerClass = UnstableApi.class)
        @Override
        public void onWrite(PageRange[] pageRanges,
                            ParcelFileDescriptor destination,
                            CancellationSignal cancellationSignal,
                            WriteResultCallback callback) {

            if (cancellationSignal.isCanceled()) {
                callback.onWriteCancelled();
                myPdfDocument.close();
                try {
                    destination.close();
                } catch (IOException e) {
                    Log.e("ViewPrintAdapter", "Error closing destination ParcelFileDescriptor", e);
                }
                return;
            }

            // For this example, we draw the entire CardView onto a single PDF page.
            // Create a Bitmap of the viewToPrint
            viewToPrint.measure(
                    View.MeasureSpec.makeMeasureSpec(pageWidth, View.MeasureSpec.AT_MOST), // Use PDF page width, allow height to wrap
                    View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED) // Let height be as needed
            );
            viewToPrint.layout(0, 0, viewToPrint.getMeasuredWidth(), viewToPrint.getMeasuredHeight());

            // Check if view's measured height is too large for a single page;
            // this example just scales it down if it is, but for real multi-page, logic would be different.
            int viewContentHeight = viewToPrint.getMeasuredHeight();
            int viewContentWidth = viewToPrint.getMeasuredWidth();

            // Create a bitmap with the view's measured dimensions
            Bitmap bitmap = Bitmap.createBitmap(viewContentWidth, viewContentHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            viewToPrint.draw(canvas);


            // Start a page
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(viewContentWidth, viewContentHeight, 1).create();
            PdfDocument.Page page = myPdfDocument.startPage(pageInfo);

            // Draw the bitmap onto the PDF page's canvas
            // You might want to scale it to fit if it's larger than the page
            Canvas pdfCanvas = page.getCanvas();
            float scale = 1.0f;
            if (viewContentWidth > pdfCanvas.getWidth()) {
                scale = (float) pdfCanvas.getWidth() / viewContentWidth;
            }
            if (viewContentHeight * scale > pdfCanvas.getHeight()) {
                scale = (float) pdfCanvas.getHeight() / viewContentHeight;
            }

            if (scale < 1.0f) {
                pdfCanvas.save();
                pdfCanvas.scale(scale, scale);
            }

            pdfCanvas.drawBitmap(bitmap, 0, 0, null);

            if (scale < 1.0f) {
                pdfCanvas.restore();
            }


            // Finish the page
            myPdfDocument.finishPage(page);


            try {
                FileOutputStream fos = new FileOutputStream(destination.getFileDescriptor());
                myPdfDocument.writeTo(fos);
                myPdfDocument.close();
                fos.close(); // Close FileOutputStream
                // destination.close(); // Framework closes this based on ParcelFileDescriptor's lifecycle

                callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});

            } catch (IOException e) {
                callback.onWriteFailed(e.toString());
                Log.e("ViewPrintAdapter", "Error writing document", e);
            } finally {
                if (myPdfDocument != null) {
                    myPdfDocument.close(); // Ensure it's closed in case of exception before writeTo
                }
                // It's generally the responsibility of the caller (framework in this case)
                // to close the ParcelFileDescriptor. Closing it prematurely here can lead to
                // "write failed: EPIPE (Broken pipe)" or similar errors.
                // However, if onWriteFailed is called, you might need to ensure it's closed.
            }
        }
    }

}