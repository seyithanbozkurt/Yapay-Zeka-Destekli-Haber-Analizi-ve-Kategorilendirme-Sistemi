package com.bitirme.dto.market;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketIndicator {
    private double value;
    private double changePercent;
}
