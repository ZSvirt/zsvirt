package org.zstack.zsv.telemetry;

import org.zstack.core.config.APIUpdateGlobalConfigMsg;
import org.zstack.core.config.GlobalConfig;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.zsv.telemetry.api.APIUpdateTelemetryConsentMsg;
import org.zstack.zsv.telemetry.header.TelemetryGlobalConfig;

import java.util.Collections;
import java.util.List;

import static org.zstack.core.Platform.err;

public class TelemetryGlobalConfigApiInterceptor implements GlobalApiMessageInterceptor {
    @Override
    public List<Class> getMessageClassToIntercept() {
        return Collections.singletonList(APIUpdateGlobalConfigMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIUpdateGlobalConfigMsg) {
            validate((APIUpdateGlobalConfigMsg) msg);
        }
        return msg;
    }

    private void validate(APIUpdateGlobalConfigMsg msg) {
        if (!TelemetryGlobalConfig.CATEGORY.equals(msg.getCategory())) {
            return;
        }

        String name = msg.getName();
        if (TelemetryGlobalConfig.CONSENT_GRANTED_AT.getName().equals(name)) {
            throw new ApiMessageInterceptionException(err(
                    TelemetryErrors.TELEMETRY_CONSENT_UPDATE_FORBIDDEN,
                    "telemetry.consent.granted.at must be updated via %s",
                    APIUpdateTelemetryConsentMsg.class.getSimpleName()));
        }

        if (TelemetryGlobalConfig.SOURCE_ID.getName().equals(name)) {
            rejectUpdateAfterInitialization(TelemetryGlobalConfig.SOURCE_ID, name);
        }

        if (TelemetryGlobalConfig.ANONYMIZATION_SALT.getName().equals(name)) {
            rejectUpdateAfterInitialization(TelemetryGlobalConfig.ANONYMIZATION_SALT, name);
        }
    }

    private void rejectUpdateAfterInitialization(GlobalConfig config, String name) {
        if (TelemetryConstant.CONSENT_NOT_GRANTED.equals(config.value())) {
            return;
        }

        throw new ApiMessageInterceptionException(err(
                TelemetryErrors.TELEMETRY_READONLY_GLOBAL_CONFIG,
                "telemetry.%s cannot be modified via UpdateGlobalConfig after initialization",
                name));
    }
}
