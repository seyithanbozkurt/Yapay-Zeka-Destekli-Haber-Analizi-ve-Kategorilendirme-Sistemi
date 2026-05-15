package com.bitirme.controller;

import com.bitirme.dto.common.ApiResponse;
import com.bitirme.dto.market.ExternalMarketResponse;
import com.bitirme.service.MarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@Tag(name = "Market", description = "Harici piyasa göstergeleri (BIST, Petrol vb.)")
public class MarketController {

    private final MarketService marketService;

    @GetMapping("/external")
    @Operation(summary = "BIST 100 ve Brent petrol göstergelerini getir")
    public ResponseEntity<ApiResponse<ExternalMarketResponse>> getExternalIndicators() {
        ExternalMarketResponse data = marketService.getExternalIndicators();
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
