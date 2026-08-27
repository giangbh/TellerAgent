package com.dnse.teller;

import com.dnse.teller.mock.MockBankingDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class MockBankingDataServiceTest {

    @Autowired
    private MockBankingDataService mockService;

    @Test
    void testDefaultCustomerSeedExists() {
        Map<String, Object> profile = mockService.getCustomerProfile("CIF-0001842");
        assertNotNull(profile);
        assertEquals("Nguyễn Minh Anh", profile.get("displayName"));
        assertEquals("PRIORITY", profile.get("segment"));

        Map<String, Object> account = mockService.resolveAccount("3456789");
        assertNotNull(account);
        assertEquals("Nguyễn Minh Anh", account.get("accountHolder"));
        assertEquals(250_000_000L, account.get("availableBalance"));
    }

    @Test
    void testDynamicSyntheticCustomerGenerationAndPersistence() {
        // Query two different CIFs
        Map<String, Object> custA1 = mockService.getCustomerProfile("CIF-556677");
        Map<String, Object> custB = mockService.getCustomerProfile("CIF-998811");

        assertNotNull(custA1);
        assertNotNull(custB);

        assertNotNull(custA1.get("displayName"));
        assertNotNull(custB.get("displayName"));

        // Second call for same CIF must return identical data (SQLite stateful consistency)
        Map<String, Object> custA2 = mockService.getCustomerProfile("CIF-556677");
        assertEquals(custA1.get("displayName"), custA2.get("displayName"));
        assertEquals(custA1.get("segment"), custA2.get("segment"));
    }

    @Test
    void testDynamicAccountResolveAndAutoGeneration() {
        String randomAccount = "987654321098";
        Map<String, Object> res1 = mockService.resolveAccount(randomAccount);
        assertNotNull(res1);
        assertNotNull(res1.get("accountHolder"));
        assertTrue(((Long) res1.get("availableBalance")) > 0);

        Map<String, Object> res2 = mockService.resolveAccount(randomAccount);
        assertEquals(res1.get("accountHolder"), res2.get("accountHolder"));
        assertEquals(res1.get("availableBalance"), res2.get("availableBalance"));
    }
}
