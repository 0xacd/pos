package com.anymind.pos.dto;

import java.math.BigDecimal;

public class HourlySaleSummary {
    private final String datetime;
    private BigDecimal sales;
    private BigDecimal points;

    public HourlySaleSummary(String datetime) {
        this.datetime = datetime;
        this.sales = BigDecimal.ZERO;
        this.points = BigDecimal.ZERO;
    }

    public void addSale(BigDecimal amount) {
        this.sales = this.sales.add(amount);
    }

    public void addPoints(BigDecimal points) {
        this.points = this.points.add(points);
    }

    public String getDatetime() { return datetime; }
    public BigDecimal getSales() { return sales; }
    public BigDecimal getPoints() { return points; }
}
