package org.zstack.zsv.telemetry;

import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.message.APIMessage;
import org.zstack.zsv.telemetry.api.APIUpdateTelemetryConsentMsg;
import org.zstack.zsv.telemetry.header.TelemetryGlobalConfig;

import static org.zstack.core.Platform.err;

@InterceptorForService("telemetry")
public class TelemetryApiInterceptor implements ApiMessageInterceptor {
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIUpdateTelemetryConsentMsg) {
            validate((APIUpdateTelemetryConsentMsg) msg);
        }
        return msg;
    }

    private void validate(APIUpdateTelemetryConsentMsg msg) {
        if (!TelemetryConstant.CONSENT_ACTION_ENABLED.equals(msg.getAction())) {
            return;
        }

        if (!TelemetryConstant.CONSENT_NOT_GRANTED.equals(TelemetryGlobalConfig.CONSENT_GRANTED_AT.value())) {
            throw new ApiMessageInterceptionException(err(
                    TelemetryErrors.TELEMETRY_ALREADY_ENABLED,
                    "Telemetry is already enabled"));
        }

        if (msg.getAgreedToTerms() == null || !msg.getAgreedToTerms()) {
            throw new ApiMessageInterceptionException(err(
                    TelemetryErrors.TELEMETRY_CONSENT_REQUIRED,
                    "User consent is required before enabling telemetry"));
        }
    }
}
