package com.dnse.teller.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutionResult {
    private String status;
    private String coreReference;
    private String postedAt;
    private Long amount;
    private String transactionType;
    private boolean mock = true;

    public ExecutionResult() {}
    public ExecutionResult(String status, String coreReference, String postedAt, Long amount, String transactionType, boolean mock) {
        this.status = status;
        this.coreReference = coreReference;
        this.postedAt = postedAt;
        this.amount = amount;
        this.transactionType = transactionType;
        this.mock = mock;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCoreReference() { return coreReference; }
    public void setCoreReference(String coreReference) { this.coreReference = coreReference; }

    public String getPostedAt() { return postedAt; }
    public void setPostedAt(String postedAt) { this.postedAt = postedAt; }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public boolean isMock() { return mock; }
    public void setMock(boolean mock) { this.mock = mock; }
}
