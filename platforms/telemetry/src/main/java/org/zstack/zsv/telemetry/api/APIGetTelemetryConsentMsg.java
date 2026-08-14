package org.zstack.zsv.telemetry.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/telemetry/consent",
        method = HttpMethod.GET,
        responseClass = APIGetTelemetryConsentReply.class
)
public class APIGetTelemetryConsentMsg extends APISyncCallMessage {
    public static APIGetTelemetryConsentMsg __example__() {
        return new APIGetTelemetryConsentMsg();
    }
}
