package org.zstack.zsv.telemetry.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.zsv.telemetry.entity.TelemetryUpdateInfoView;

@RestResponse(allTo = "inventory")
public class APICheckTelemetryUpdateEvent extends APIEvent {
    private TelemetryUpdateInfoView inventory;

    public APICheckTelemetryUpdateEvent() {
        super(null);
    }

    public APICheckTelemetryUpdateEvent(String apiId) {
        super(apiId);
    }

    public TelemetryUpdateInfoView getInventory() {
        return inventory;
    }

    public void setInventory(TelemetryUpdateInfoView inventory) {
        this.inventory = inventory;
    }

    public static APICheckTelemetryUpdateEvent __example__() {
        APICheckTelemetryUpdateEvent event = new APICheckTelemetryUpdateEvent();
        event.setInventory(TelemetryUpdateInfoView.__example__());
        return event;
    }
}
