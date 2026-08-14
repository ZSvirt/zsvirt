package org.zstack.core.keyvalue;

/**
 */
public enum Op {
    EQ("="),
    NOT_EQ("!="),
    NOT_NULL("is not null"),
    NULL("is null"),
    IN("in"),
    NOT_IN("not in"),
    GT(">"),
    LT("<"),
    GTE(">="),
    LTE("<="),
    LIKE("like"),
    NOT_LIKE("not like");

    private String symbol;

    private Op(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return symbol;
    }
}
