package org.zstack.zwatch.alarm;

import static org.zstack.core.Platform.i18n;

public enum AlarmStatus {
    OK,
    Alarm,
    InsufficientData;

    public String toi18nString() {
        if (this.equals(OK)) {
            return i18n("OK");
        } else if (this.equals(Alarm)) {
            return i18n("Alarm");
        } else if (this.equals(InsufficientData)) {
            return i18n("InsufficientData");
        } else {
            return this.toString();
        }
    }
}
