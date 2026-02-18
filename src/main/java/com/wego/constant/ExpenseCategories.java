package com.wego.constant;

import java.util.List;

/**
 * Constants for expense categories, shared across team and personal expenses.
 * 7 categories total: 5 existing + 2 new (SHOPPING, HEALTH).
 */
public final class ExpenseCategories {

    private ExpenseCategories() {}

    public static final String FOOD = "FOOD";
    public static final String TRANSPORT = "TRANSPORT";
    public static final String ACCOMMODATION = "ACCOMMODATION";
    public static final String SHOPPING = "SHOPPING";
    public static final String ENTERTAINMENT = "ENTERTAINMENT";
    public static final String HEALTH = "HEALTH";
    public static final String OTHER = "OTHER";

    /** All categories in display order. */
    public static final List<String> ALL = List.of(
            FOOD, TRANSPORT, ACCOMMODATION, SHOPPING, ENTERTAINMENT, HEALTH, OTHER);
}
