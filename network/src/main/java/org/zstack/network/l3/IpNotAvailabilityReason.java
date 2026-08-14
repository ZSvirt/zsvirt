package org.zstack.network.l3;

import static org.zstack.core.Platform.i18n;

public enum IpNotAvailabilityReason {
    GATEWAY("it is gateway"),
    NO_IN_RANGE("it is not in this range"),
    L3_NO_IP_RANGE("l3 does not have ip range"),
    USED("it is used");

    String type;

    IpNotAvailabilityReason(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
