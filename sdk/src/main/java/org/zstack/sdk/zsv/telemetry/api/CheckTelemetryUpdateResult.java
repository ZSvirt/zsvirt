package org.zstack.sdk.zsv.telemetry.api;

import org.zstack.sdk.zsv.telemetry.entity.TelemetryUpdateInfoView;

public class CheckTelemetryUpdateResult {
    public TelemetryUpdateInfoView inventory;
    public void setInventory(TelemetryUpdateInfoView inventory) {
        this.inventory = inventory;
    }
    public TelemetryUpdateInfoView getInventory() {
        return this.inventory;
    }

}
