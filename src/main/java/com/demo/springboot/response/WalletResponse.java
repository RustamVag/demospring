package com.demo.springboot.response;

import com.demo.springboot.enums.OperationType;

import java.util.UUID;

public class WalletResponse {
    private UUID walletId;
    private OperationType operationType;
    private double amount;

    // Constructor, Getters, and Setters
    public WalletResponse(UUID walletId, OperationType operationType, double amount) {
        this.walletId = walletId;
        this.operationType = operationType;
        this.amount = amount;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public void setWalletId(UUID walletId) {
        this.walletId = walletId;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}