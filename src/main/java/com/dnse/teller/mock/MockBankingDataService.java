package com.dnse.teller.mock;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * MockBankingDataService:
 * Kiến trúc Facade thống nhất điều phối các Domain Service chuyên biệt:
 * - MockCustomerProfileService: Quản lý thông tin định danh, KYC, phân khúc
 * - MockAccountService: Quản lý tài khoản thanh toán, tiền gửi, đối soát STK
 * - MockTransactionStatementService: Trích lục 10-30 giao dịch sao kê phong phú
 * - MockCustomerPersonaService: Phân tích chân dung 360, khẩu vị đầu tư
 * - MockCreditScoreService: Chấm điểm tín dụng, tra cứu CIC
 * - MockNextBestOfferService: Đề xuất ưu đãi & sản phẩm tiếp theo (NBO)
 * - MockSavingsAdvisorService: Cố vấn tối ưu lãi suất tiền gửi
 * - MockCardService: Quản trị dịch vụ thẻ ATM, Visa, Mastercard
 * - MockMarketDirectoryService: Tỷ giá ngoại tệ & mạng lưới chi nhánh
 */
@Service
public class MockBankingDataService {

    private final JdbcTemplate jdbcTemplate;
    private final MockCustomerProfileService customerProfileService;
    private final MockAccountService accountService;
    private final MockTransactionStatementService statementService;
    private final MockCustomerPersonaService personaService;
    private final MockCreditScoreService creditScoreService;
    private final MockNextBestOfferService nextBestOfferService;
    private final MockSavingsAdvisorService savingsAdvisorService;
    private final MockCardService cardService;
    private final MockMarketDirectoryService marketDirectoryService;

    public MockBankingDataService(
            JdbcTemplate jdbcTemplate,
            MockCustomerProfileService customerProfileService,
            MockAccountService accountService,
            MockTransactionStatementService statementService,
            MockCustomerPersonaService personaService,
            MockCreditScoreService creditScoreService,
            MockNextBestOfferService nextBestOfferService,
            MockSavingsAdvisorService savingsAdvisorService,
            MockCardService cardService,
            MockMarketDirectoryService marketDirectoryService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.customerProfileService = customerProfileService;
        this.accountService = accountService;
        this.statementService = statementService;
        this.personaService = personaService;
        this.creditScoreService = creditScoreService;
        this.nextBestOfferService = nextBestOfferService;
        this.savingsAdvisorService = savingsAdvisorService;
        this.cardService = cardService;
        this.marketDirectoryService = marketDirectoryService;
    }

    @PostConstruct
    public void init() {
        seedDefaultCustomer();
    }

    public synchronized void seedDefaultCustomer() {
        String defaultCif = "CIF-0001842";
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mock_customers WHERE cif = ?", Integer.class, defaultCif);

        if (count == null || count == 0) {
            jdbcTemplate.update("""
                INSERT INTO mock_customers (cif, display_name, kyc_status, segment, risk_rating, identity_masked, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """, defaultCif, "Nguyễn Minh Anh", "VERIFIED", "PRIORITY", "LOW", "0123••••89", Instant.now().toString());

            // Add default accounts
            jdbcTemplate.update("""
                INSERT INTO mock_accounts (account_number, cif, account_ref, currency, available_balance, account_type, term_months, interest_rate, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, "3456789", defaultCif, "ACC-001", "VND", 250_000_000L, "CHECKING", 0, 0.0, "ACTIVE");

            jdbcTemplate.update("""
                INSERT INTO mock_accounts (account_number, cif, account_ref, currency, available_balance, account_type, term_months, interest_rate, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, "012345678901", defaultCif, "ACC-002", "VND", 42_500_000L, "CHECKING", 0, 0.0, "ACTIVE");

            jdbcTemplate.update("""
                INSERT INTO mock_accounts (account_number, cif, account_ref, currency, available_balance, account_type, term_months, interest_rate, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, "TD-888999", defaultCif, "ACC-TD-01", "VND", 1_500_000_000L, "TERM_DEPOSIT", 6, 5.8, "ACTIVE");
        }
    }

    public String normalizeCif(String customerRef) {
        return customerProfileService.normalizeCif(customerRef);
    }

    public void ensureCustomerExists(String cif) {
        customerProfileService.ensureCustomerExists(cif);
    }

    public Map<String, Object> getCustomerProfile(String customerRef) {
        return customerProfileService.getCustomerProfile(customerRef);
    }

    public Map<String, Object> getCustomerAccounts(String customerRef) {
        return accountService.getCustomerAccounts(customerRef);
    }

    public Map<String, Object> getCustomerAccountsSummary(String customerRef) {
        return accountService.getCustomerAccountsSummary(customerRef);
    }

    public Map<String, Object> resolveAccount(String accountNumber) {
        return accountService.resolveAccount(accountNumber);
    }

    public Map<String, Object> getTransactionHistory(String accountNumber, String customerRef) {
        return statementService.getTransactionHistory(accountNumber, customerRef);
    }

    public Map<String, Object> getTransactionHistory(String accountNumber, String customerRef, int count) {
        return statementService.getTransactionHistory(accountNumber, customerRef, count);
    }

    public Map<String, Object> getCustomerPersona(String customerRef) {
        return personaService.getCustomerPersona(customerRef);
    }

    public Map<String, Object> getCustomerCreditScore(String customerRef) {
        return creditScoreService.getCustomerCreditScore(customerRef);
    }

    public Map<String, Object> getNextBestOffers(String customerRef) {
        return nextBestOfferService.getNextBestOffers(customerRef);
    }

    public Map<String, Object> calculateSavingsYield(long amount, int termMonths) {
        return savingsAdvisorService.calculateSavingsYield(amount, termMonths);
    }

    public Map<String, Object> adviseSavingsPlan(long amount, int termMonths) {
        return savingsAdvisorService.adviseSavingsPlan(amount, termMonths);
    }

    public Map<String, Object> getCardServices(String customerRef) {
        return cardService.getCardServices(customerRef);
    }

    public Map<String, Object> lookupFxRate(String currency, long amount) {
        return marketDirectoryService.lookupFxRate(currency, amount);
    }

    public Map<String, Object> lookupBranch(String query) {
        return marketDirectoryService.lookupBranch(query);
    }

    // Getters for specific specialized domain services
    public MockCustomerProfileService getCustomerProfileService() { return customerProfileService; }
    public MockAccountService getAccountService() { return accountService; }
    public MockTransactionStatementService getStatementService() { return statementService; }
    public MockCustomerPersonaService getPersonaService() { return personaService; }
    public MockCreditScoreService getCreditScoreService() { return creditScoreService; }
    public MockNextBestOfferService getNextBestOfferService() { return nextBestOfferService; }
    public MockSavingsAdvisorService getSavingsAdvisorService() { return savingsAdvisorService; }
    public MockCardService getCardService() { return cardService; }
    public MockMarketDirectoryService getMarketDirectoryService() { return marketDirectoryService; }
}
