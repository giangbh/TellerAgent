package com.dnse.teller.controller;

import com.dnse.teller.mock.MockBankingDataService;
import com.dnse.teller.search.GlobalSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GlobalSearchController {

    private final GlobalSearchService searchService;
    private final MockBankingDataService bankingDataService;

    public GlobalSearchController(GlobalSearchService searchService, MockBankingDataService bankingDataService) {
        this.searchService = searchService;
        this.bankingDataService = bankingDataService;
    }

    @GetMapping("/search/global")
    public ResponseEntity<Map<String, Object>> globalSearch(@RequestParam(value = "q", defaultValue = "") String query) {
        return ResponseEntity.ok(searchService.search(query));
    }

    @GetMapping("/accounts/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getAccountDetail(@PathVariable String accountNumber) {
        Map<String, Object> acc = bankingDataService.resolveAccount(accountNumber);
        if (acc == null || acc.containsKey("error")) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(acc);
    }

    @GetMapping("/customers/{cif}/360")
    public ResponseEntity<Map<String, Object>> getCustomer360(@PathVariable String cif) {
        String normalizedCif = bankingDataService.normalizeCif(cif);
        bankingDataService.ensureCustomerExists(normalizedCif);

        Map<String, Object> profile = bankingDataService.getCustomerProfile(normalizedCif);
        Map<String, Object> accounts = bankingDataService.getCustomerAccounts(normalizedCif);
        Map<String, Object> accountsSummary = bankingDataService.getCustomerAccountsSummary(normalizedCif);
        Map<String, Object> creditScore = bankingDataService.getCustomerCreditScore(normalizedCif);
        Map<String, Object> nbo = bankingDataService.getNextBestOffers(normalizedCif);
        Map<String, Object> history = bankingDataService.getTransactionHistory(null, normalizedCif, 5);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cif", normalizedCif);
        result.put("profile", profile);
        result.put("accounts", accounts);
        result.put("accountsSummary", accountsSummary);
        result.put("creditScore", creditScore);
        result.put("nextBestOffers", nbo);
        result.put("recentTransactions", history.get("transactions"));

        return ResponseEntity.ok(result);
    }
}

