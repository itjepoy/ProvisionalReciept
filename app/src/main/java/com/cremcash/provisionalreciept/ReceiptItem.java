package com.cremcash.provisionalreciept;

public class ReceiptItem {
    public String receiptNo;
    public String payor;
    public String amountInWords;
    public String formOfPayment;
    public double totalAmount;
    public String receivedBy;
    public String dateReceived;

    public ReceiptItem(String receiptNo, String payor, String amountInWords,
                       String formOfPayment, double totalAmount,
                       String receivedBy, String dateReceived) {
        this.receiptNo = receiptNo;
        this.payor = payor;
        this.amountInWords = amountInWords;
        this.formOfPayment = formOfPayment;
        this.totalAmount = totalAmount;
        this.receivedBy = receivedBy;
        this.dateReceived = dateReceived;
    }

    // ✅ Add these getters
    public String getReceiptNo() { return receiptNo; }
    public String getPayor() { return payor; }
    public String getAmountInWords() { return amountInWords; }
    public String getFormOfPayment() { return formOfPayment; }
    public double getTotalAmount() { return totalAmount; }
    public String getReceivedBy() { return receivedBy; }
    public String getDateReceived() { return dateReceived; }
}
