package org.zstack.drs.data;

import org.zstack.drs.DRSConstants;

/**
 * Created by lining on 2018/9/4.
 */
public enum DRSAutomationLevel {
    Automatic(DRSConstants.AUTOMATION_LEVEL_AUTOMATIC),
    Manual(DRSConstants.AUTOMATION_LEVEL_MANUAL);

    private String name;

    DRSAutomationLevel(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
