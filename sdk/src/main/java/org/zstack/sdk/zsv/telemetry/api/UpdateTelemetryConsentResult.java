package org.zstack.sdk.zsv.telemetry.api;

import org.zstack.sdk.zsv.telemetry.entity.TelemetryConsentView;

public class UpdateTelemetryConsentResult {
    public TelemetryConsentView inventory;
    public void setInventory(TelemetryConsentView inventory) {
        this.inventory = inventory;
    }
    public TelemetryConsentView getInventory() {
        return this.inventory;
    }

}
