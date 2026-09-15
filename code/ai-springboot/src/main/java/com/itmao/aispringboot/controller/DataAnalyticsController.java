package com.itmao.aispringboot.controller;

import com.itmao.aispringboot.common.Result;
import com.itmao.aispringboot.service.DataAnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/data-analytics")
public class DataAnalyticsController {

    private final DataAnalyticsService dataAnalyticsService;

    public DataAnalyticsController(DataAnalyticsService dataAnalyticsService) {
        this.dataAnalyticsService = dataAnalyticsService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(dataAnalyticsService.overview());
    }
}
