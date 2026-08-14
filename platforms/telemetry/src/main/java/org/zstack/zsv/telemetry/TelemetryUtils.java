package org.zstack.zsv.telemetry;

import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.zsv.telemetry.header.TelemetryGlobalConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TelemetryUtils {
    public static boolean isConsentGranted() {
        return !TelemetryConstant.CONSENT_NOT_GRANTED.equals(TelemetryGlobalConfig.CONSENT_GRANTED_AT.value());
    }

    public static String generateAnonymizationSalt() {
        return Platform.getUuid() + Platform.getUuid();
    }

    public static boolean isCoordinator() {
        List<String> uuids = listManagementNodeUuids();
        if (uuids.isEmpty()) {
            return true;
        }
        Collections.sort(uuids);
        return Objects.equals(uuids.get(0), Platform.getManagementServerId());
    }

    public static String electRandomManagementNode() {
        List<String> uuids = listManagementNodeUuids();
        if (uuids.isEmpty()) {
            return Platform.getManagementServerId();
        }
        int index = Math.floorMod(Platform.getUuid().hashCode(), uuids.size());
        return uuids.get(index);
    }

    public static List<String> listManagementNodeUuids() {
        List<String> uuids = Q.New(ManagementNodeVO.class).select(ManagementNodeVO_.uuid).listValues();
        return uuids == null ? new ArrayList<>() : new ArrayList<>(uuids);
    }
}
