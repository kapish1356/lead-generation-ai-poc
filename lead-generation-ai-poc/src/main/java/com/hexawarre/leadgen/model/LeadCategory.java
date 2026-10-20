package com.hexawarre.leadgen.model;

public enum LeadCategory {
    HOT,
    WARM,
    COLD;

    public static LeadCategory fromScore(int score) {
        if (score >= 70) {
            return HOT;
        }
        if (score >= 40) {
            return WARM;
        }
        return COLD;
    }
}
