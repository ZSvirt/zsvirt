package org.zstack.zsv.telemetry.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.zsv.telemetry.entity.TelemetryConsentView;

@RestResponse(allTo = "inventory")
public class APIUpdateTelemetryConsentEvent extends APIEvent {
    private TelemetryConsentView inventory;

    public APIUpdateTelemetryConsentEvent() {
        super(null);
    }

    public APIUpdateTelemetryConsentEvent(String apiId) {
        super(apiId);
    }

    public TelemetryConsentView getInventory() {
        return inventory;
    }

    public void setInventory(TelemetryConsentView inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateTelemetryConsentEvent __example__() {
        APIUpdateTelemetryConsentEvent event = new APIUpdateTelemetryConsentEvent();
        TelemetryConsentView view = new TelemetryConsentView();
        view.setConsentGrantedAt("2026-07-07T07:51Z");
        event.setInventory(view);
        return event;
    }
}
