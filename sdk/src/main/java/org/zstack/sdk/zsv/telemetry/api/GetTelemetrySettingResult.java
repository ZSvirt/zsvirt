package org.zstack.sdk.zsv.telemetry.api;

import org.zstack.sdk.zsv.telemetry.entity.TelemetrySettingView;

public class GetTelemetrySettingResult {
    public TelemetrySettingView inventory;
    public void setInventory(TelemetrySettingView inventory) {
        this.inventory = inventory;
    }
    public TelemetrySettingView getInventory() {
        return this.inventory;
    }

}
