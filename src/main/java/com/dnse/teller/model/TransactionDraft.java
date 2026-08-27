package com.dnse.teller.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionDraft {
    private String screenCode;
    private String screenTitle;
    private String transactionType;
    private String sourceAccountRef;
    private String sourceAccountMasked;
    private String accountRef;
    private String accountNumber;
    private String accountNoMasked;
    private String accountHolder;
    private String accountStatus;
    private Long availableBalance;
    private String beneficiaryAccount;
    private String beneficiaryName;
    private String bankCode;
    private String bankName;
    private Long amount;
    private Object fee;
    private String currency = "VND";
    private String description;
    private Map<String, Object> limit;
    private ValidationResult validation;

    public TransactionDraft() {}

    public String getScreenCode() { return screenCode; }
    public void setScreenCode(String screenCode) { this.screenCode = screenCode; }

    public String getScreenTitle() { return screenTitle; }
    public void setScreenTitle(String screenTitle) { this.screenTitle = screenTitle; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getSourceAccountRef() { return sourceAccountRef; }
    public void setSourceAccountRef(String sourceAccountRef) { this.sourceAccountRef = sourceAccountRef; }

    public String getSourceAccountMasked() { return sourceAccountMasked; }
    public void setSourceAccountMasked(String sourceAccountMasked) { this.sourceAccountMasked = sourceAccountMasked; }

    public String getAccountRef() { return accountRef; }
    public void setAccountRef(String accountRef) { this.accountRef = accountRef; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountNoMasked() { return accountNoMasked; }
    public void setAccountNoMasked(String accountNoMasked) { this.accountNoMasked = accountNoMasked; }

    public String getAccountHolder() { return accountHolder; }
    public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public Long getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(Long availableBalance) { this.availableBalance = availableBalance; }

    public String getBeneficiaryAccount() { return beneficiaryAccount; }
    public void setBeneficiaryAccount(String beneficiaryAccount) { this.beneficiaryAccount = beneficiaryAccount; }

    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public Object getFee() { return fee; }
    public void setFee(Object fee) { this.fee = fee; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, Object> getLimit() { return limit; }
    public void setLimit(Map<String, Object> limit) { this.limit = limit; }

    public ValidationResult getValidation() { return validation; }
    public void setValidation(ValidationResult validation) { this.validation = validation; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationResult {
        private Boolean valid;
        private List<String> missingFields = new ArrayList<>();
        private List<String> blockers = new ArrayList<>();

        public ValidationResult() {}
        public ValidationResult(Boolean valid, List<String> missingFields, List<String> blockers) {
            this.valid = valid;
            this.missingFields = missingFields != null ? missingFields : new ArrayList<>();
            this.blockers = blockers != null ? blockers : new ArrayList<>();
        }

        public Boolean getValid() { return valid; }
        public void setValid(Boolean valid) { this.valid = valid; }

        public List<String> getMissingFields() { return missingFields; }
        public void setMissingFields(List<String> missingFields) { this.missingFields = missingFields; }

        public List<String> getBlockers() { return blockers; }
        public void setBlockers(List<String> blockers) { this.blockers = blockers; }
    }
}
