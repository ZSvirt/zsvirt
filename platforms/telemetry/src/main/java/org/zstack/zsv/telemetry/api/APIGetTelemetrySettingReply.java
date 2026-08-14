package org.zstack.zsv.telemetry.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zsv.telemetry.entity.TelemetrySettingView;

@RestResponse(allTo = "inventory")
public class APIGetTelemetrySettingReply extends APIReply {
    private TelemetrySettingView inventory;

    public TelemetrySettingView getInventory() {
        return inventory;
    }

    public void setInventory(TelemetrySettingView inventory) {
        this.inventory = inventory;
    }

    public static APIGetTelemetrySettingReply __example__() {
        APIGetTelemetrySettingReply reply = new APIGetTelemetrySettingReply();
        reply.setInventory(TelemetrySettingView.__example__());
        return reply;
    }
}
