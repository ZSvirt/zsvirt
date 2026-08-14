package org.zstack.zsv.telemetry.config;

import org.zstack.core.Platform;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigVO;
import org.zstack.core.config.GlobalConfigVO_;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.zsv.telemetry.TelemetryConstant;
import org.zstack.zsv.telemetry.TelemetryUtils;
import org.zstack.zsv.telemetry.header.TelemetryGlobalConfig;

public class TelemetryGlobalConfigInitExtensions implements ManagementNodeReadyExtensionPoint {
    @Override
    public void managementNodeReady() {
        repairNullDefaultValue(TelemetryGlobalConfig.SOURCE_ID);
        repairNullDefaultValue(TelemetryGlobalConfig.ANONYMIZATION_SALT);
        initIfAbsent(TelemetryGlobalConfig.SOURCE_ID, Platform.getManagementServerId());
        initIfAbsent(TelemetryGlobalConfig.ANONYMIZATION_SALT, TelemetryUtils.generateAnonymizationSalt());
    }

    private void repairNullDefaultValue(GlobalConfig config) {
        GlobalConfigVO vo = Q.New(GlobalConfigVO.class)
                .eq(GlobalConfigVO_.category, config.getCategory())
                .eq(GlobalConfigVO_.name, config.getName())
                .find();
        if (vo == null || vo.getDefaultValue() != null) {
            return;
        }

        SQL.New(GlobalConfigVO.class)
                .eq(GlobalConfigVO_.category, config.getCategory())
                .eq(GlobalConfigVO_.name, config.getName())
                .set(GlobalConfigVO_.defaultValue, TelemetryConstant.CONSENT_NOT_GRANTED)
                .update();
    }

    private void initIfAbsent(GlobalConfig config, String value) {
        if (value == null) {
            return;
        }

        if (TelemetryConstant.CONSENT_NOT_GRANTED.equals(config.value())) {
            config.updateValue(value);
        }
    }
}
