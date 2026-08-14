package org.zstack.zsv.telemetry.privacy;

import org.apache.commons.codec.digest.DigestUtils;
import org.zstack.zsv.telemetry.TelemetryConstant;
import org.zstack.zsv.telemetry.header.TelemetryGlobalConfig;

public class TelemetryAnonymizer {
    public String anonymize(String uuid) {
        String salt = TelemetryGlobalConfig.ANONYMIZATION_SALT.value();
        return DigestUtils.sha256Hex(uuid + salt);
    }

    public String anonymize(String uuid, String salt) {
        return DigestUtils.sha256Hex(uuid + salt);
    }

    public boolean isSaltInitialized() {
        return !TelemetryConstant.CONSENT_NOT_GRANTED.equals(TelemetryGlobalConfig.ANONYMIZATION_SALT.value());
    }
}
