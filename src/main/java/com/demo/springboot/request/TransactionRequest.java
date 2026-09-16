package com.demo.springboot.request;

import com.demo.springboot.enums.OperationType;

import java.math.BigDecimal;
import java.util.UUID;

public class TransactionRequest {
    private UUID walletId;

    private OperationType operationType;

    private BigDecimal amount;

    public TransactionRequest(UUID id, OperationType operationType, BigDecimal bigDecimal) {
        this.walletId = id;
        this.operationType = operationType;
        this.amount = bigDecimal;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setWalletId(UUID walletId) {
        this.walletId = walletId;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
