package org.zstack.zsv.telemetry.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.Component;
import org.zstack.zsv.telemetry.TelemetryConstant;
import org.zstack.zsv.telemetry.TelemetryErrors;
import org.zstack.zsv.telemetry.TelemetryTaskScheduler;
import org.zstack.zsv.telemetry.header.TelemetryGlobalConfig;

import static org.zstack.core.Platform.err;

public class TelemetryConsentGlobalConfigExtensions implements Component {
    @Autowired
    private TelemetryTaskScheduler taskScheduler;

    @Override
    public boolean start() {
        installConsentGrantedAtExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void installConsentGrantedAtExtensions() {
        TelemetryGlobalConfig.CONSENT_GRANTED_AT.installLocalBeforeUpdateExtension(
                (oldConfig, newValue) -> validateConsentTransition(oldConfig.value(), newValue));

        TelemetryGlobalConfig.CONSENT_GRANTED_AT.installUpdateExtension(
                (oldConfig, newConfig) -> taskScheduler.syncTasksWithConsent());
    }

    static void validateConsentTransition(String oldValue, String newValue) {
        if (TelemetryConstant.CONSENT_NOT_GRANTED.equals(newValue)) {
            return;
        }

        if (!TelemetryConstant.CONSENT_NOT_GRANTED.equals(oldValue)) {
            throw err(TelemetryErrors.TELEMETRY_ALREADY_ENABLED, "Telemetry is already enabled").toException();
        }
    }
}
