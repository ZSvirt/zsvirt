package org.zstack.zsv.telemetry.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.zsv.telemetry.TelemetryConstant;

@RestRequest(
        path = "/telemetry/consent",
        method = HttpMethod.PUT,
        responseClass = APIUpdateTelemetryConsentEvent.class,
        isAction = true
)
public class APIUpdateTelemetryConsentMsg extends APIMessage {
    @APIParam(validValues = {TelemetryConstant.CONSENT_ACTION_ENABLED, TelemetryConstant.CONSENT_ACTION_DISABLED})
    private String action;

    @APIParam(required = false)
    private Boolean agreedToTerms;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Boolean getAgreedToTerms() {
        return agreedToTerms;
    }

    public void setAgreedToTerms(Boolean agreedToTerms) {
        this.agreedToTerms = agreedToTerms;
    }

    public static APIUpdateTelemetryConsentMsg __example__() {
        APIUpdateTelemetryConsentMsg msg = new APIUpdateTelemetryConsentMsg();
        msg.setAction(TelemetryConstant.CONSENT_ACTION_ENABLED);
        msg.setAgreedToTerms(true);
        return msg;
    }
}
