package com.beyond.pochaon.ingredient.domain;

public enum StockStatus {
    RED("위험", "재고가 안전재고의 50% 미만입니다."),
    YELLOW("주의", "재고가 안전재고 수준입니다."),
    GREEN("정상", "재고가 충분합니다.");

    private final String label;
    private final String description;

    StockStatus(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() { return label; }
    public String getDescription() { return description; }
}
