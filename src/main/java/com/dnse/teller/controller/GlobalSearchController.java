package com.dnse.teller.controller;

import com.dnse.teller.search.GlobalSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class GlobalSearchController {

    private final GlobalSearchService searchService;

    public GlobalSearchController(GlobalSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/global")
    public ResponseEntity<Map<String, Object>> globalSearch(@RequestParam(value = "q", defaultValue = "") String query) {
        return ResponseEntity.ok(searchService.search(query));
    }
}
