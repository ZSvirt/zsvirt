package org.zstack.zwatch.ruleengine;

import org.zstack.header.exception.CloudRuntimeException;

import static org.zstack.core.Platform.i18n;

public enum ComparisonOperator {
    GreaterThanOrEqualTo,
    GreaterThan,
    LessThan,
    LessThanOrEqualTo;

    public ComparisonOperator getReverseOperator() {
        if (this == GreaterThanOrEqualTo) {
            return LessThan;
        } else if (this == GreaterThan) {
            return LessThanOrEqualTo;
        } else if (this == LessThan) {
            return GreaterThanOrEqualTo;
        } else if (this == LessThanOrEqualTo) {
            return GreaterThan;
        }

        throw new CloudRuntimeException(String.format("%s has no reverse operator", this));
    }

    public String toi18nString() {
        if (this.equals(GreaterThanOrEqualTo)) {
            return i18n("GreaterThanOrEqualTo");
        } else if (this.equals(GreaterThan)) {
            return i18n("GreaterThan");
        } else if (this.equals(LessThan)) {
            return i18n("LessThan");
        } else if (this.equals(LessThanOrEqualTo)) {
            return i18n("LessThanOrEqualTo");
        } else {
            return this.toString();
        }
    }
}
