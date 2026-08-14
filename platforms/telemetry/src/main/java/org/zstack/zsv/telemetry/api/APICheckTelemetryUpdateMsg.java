package org.zstack.zsv.telemetry.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/telemetry/updates/check",
        method = HttpMethod.PUT,
        responseClass = APICheckTelemetryUpdateEvent.class,
        isAction = true
)
public class APICheckTelemetryUpdateMsg extends APIMessage {
    public static APICheckTelemetryUpdateMsg __example__() {
        return new APICheckTelemetryUpdateMsg();
    }
}
