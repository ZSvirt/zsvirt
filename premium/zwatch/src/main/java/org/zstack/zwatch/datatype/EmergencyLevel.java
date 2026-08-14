package org.zstack.zwatch.datatype;

import static org.zstack.core.Platform.i18n;

/**
 * Created by lining on 2019/11/27.
 */
public enum EmergencyLevel {
    Emergent,
    Important,
    Normal;

    public String toi18nString() {
        if (this.equals(Emergent)) {
            return i18n("Emergent");
        } else if (this.equals(Important)) {
            return i18n("Important");
        } else if (this.equals(Normal)) {
            return i18n("Normal");
        } else {
            return this.toString();
        }
    }
}
