package org.zstack.zsv.telemetry.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zsv.telemetry.TelemetryConstant;
import org.zstack.zsv.telemetry.entity.TelemetryConsentView;

@RestResponse(allTo = "inventory")
public class APIGetTelemetryConsentReply extends APIReply {
    private TelemetryConsentView inventory;

    public TelemetryConsentView getInventory() {
        return inventory;
    }

    public void setInventory(TelemetryConsentView inventory) {
        this.inventory = inventory;
    }

    public static APIGetTelemetryConsentReply __example__() {
        APIGetTelemetryConsentReply reply = new APIGetTelemetryConsentReply();
        TelemetryConsentView view = new TelemetryConsentView();
        view.setConsentGrantedAt(TelemetryConstant.CONSENT_NOT_GRANTED);
        reply.setInventory(view);
        return reply;
    }
}
