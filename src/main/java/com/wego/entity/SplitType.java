package com.wego.entity;

/**
 * Enum representing how an expense is split among members.
 *
 * @see Expense
 * @see ExpenseSplit
 */
public enum SplitType {

    /**
     * Split equally among all selected members.
     */
    EQUAL,

    /**
     * Custom amounts specified for each member.
     */
    CUSTOM,

    /**
     * Split by percentage.
     */
    PERCENTAGE,

    /**
     * Split by shares (e.g., 2:1:1).
     */
    SHARES
}
