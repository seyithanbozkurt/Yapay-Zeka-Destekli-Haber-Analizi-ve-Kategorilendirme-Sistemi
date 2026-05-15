package com.bitirme.dto.market;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalMarketResponse {
    private MarketIndicator bist100;
    private MarketIndicator brent;
}
