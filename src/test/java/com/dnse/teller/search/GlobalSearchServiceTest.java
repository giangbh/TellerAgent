package com.dnse.teller.search;

import com.dnse.teller.mock.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GlobalSearchServiceTest {

    @Autowired
    private GlobalSearchService searchService;

    @Test
    void testSearchSpecificCifPrefix() {
        // Gõ "cif: 111111" phải trả về kết quả đúng với 111111, tuyệt đối không trả về CIF-0001842
        Map<String, Object> res = searchService.search("cif: 111111");
        List<?> categories = (List<?>) res.get("categories");

        assertNotNull(categories);
        // Kiểm tra tất cả items trả về đều phải chứa "111111"
        for (Object catObj : categories) {
            GlobalSearchService.SearchResultCategory cat = (GlobalSearchService.SearchResultCategory) catObj;
            for (GlobalSearchService.SearchResultItem item : cat.getItems()) {
                assertTrue(item.getId().contains("111111") || item.getTitle().contains("111111"),
                        "Kết quả tìm CIF 111111 không được chứa CIF khác: " + item.getId());
                assertFalse(item.getId().contains("0001842"), "Không được trả về CIF-0001842");
            }
        }
    }

    @Test
    void testSearchCifExact() {
        Map<String, Object> res = searchService.search("cif: 0001842");
        List<?> categories = (List<?>) res.get("categories");
        assertFalse(categories.isEmpty());

        GlobalSearchService.SearchResultCategory cat = (GlobalSearchService.SearchResultCategory) categories.get(0);
        assertEquals("CUSTOMERS", cat.getType());
        assertEquals("CIF-0001842", cat.getItems().get(0).getId());
    }

    @Test
    void testSearchSpecificAccountPrefix() {
        Map<String, Object> res = searchService.search("stk: 3456789");
        List<?> categories = (List<?>) res.get("categories");
        assertFalse(categories.isEmpty());

        GlobalSearchService.SearchResultCategory cat = (GlobalSearchService.SearchResultCategory) categories.get(0);
        assertEquals("ACCOUNTS", cat.getType());
        assertEquals("3456789", cat.getItems().get(0).getId());
    }
}
