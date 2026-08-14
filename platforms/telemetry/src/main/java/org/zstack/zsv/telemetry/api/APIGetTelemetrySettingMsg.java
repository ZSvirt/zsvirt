package org.zstack.zsv.telemetry.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/telemetry/settings",
        method = HttpMethod.GET,
        responseClass = APIGetTelemetrySettingReply.class
)
public class APIGetTelemetrySettingMsg extends APISyncCallMessage {
    public static APIGetTelemetrySettingMsg __example__() {
        return new APIGetTelemetrySettingMsg();
    }
}
