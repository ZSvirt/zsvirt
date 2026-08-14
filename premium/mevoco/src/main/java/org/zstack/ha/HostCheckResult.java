package org.zstack.ha;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.storage.primary.PrimaryStorageType;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.zstack.core.Platform.i18n;

public class HostCheckResult {
    private static final CLogger logger = Utils.getLogger(HostCheckResult.class);

    AtomicInteger weightTotal = new AtomicInteger();
    AtomicInteger weight = new AtomicInteger();
    List<ErrorCode> errors = new ArrayList<>();
    String details;
    private HostScanResult result = null;
    List<String> vmUuids = new ArrayList<>();
    Map<PrimaryStorageType, Boolean> isCheckVmOnHost = new ConcurrentHashMap<>();

    public static class HostScanResult {
        private boolean alive;
        private String evidence;

        private HostScanResult(boolean alive, String evidence) {
            this.alive = alive;
            this.evidence = evidence;
        }

        private static HostScanResult ofAlive(String evidence) {
            return new HostScanResult(true, evidence);
        }

        private static HostScanResult ofDead(String evidence) {
            return new HostScanResult(false, evidence);
        }

        public boolean isAlive() {
            return alive;
        }

        public boolean isDead() {
            return !isAlive();
        }

        public String getEvidence() {
            return evidence;
        }
    }

    synchronized HostScanResult getCheckResult(String hostUuid) {
        if (result != null) {
            return result;
        }

        logger.info(String.format("[HA worker]: checked host %s", hostUuid));
        result = HostScanResult.ofAlive("checking");

        if (isCheckVmOnHost.containsValue(Boolean.TRUE) || vmUuids != null && !vmUuids.isEmpty()) {
            result.evidence = details = String.format("the host[uuid:%s] is judged as alive", hostUuid);
            return result;
        }

        if (weightTotal.get() == 0) {
            result.evidence = details = "[HA worker]: No way to check the host status";
            return result;
        }

        logger.info(String.format("[HA worker]: checker completed, weight: %d, total: %d", weight.get(), weightTotal.get()));

        float ratio = weight.get() / (float) weightTotal.get();
        float threshold = HaGlobalConfig.CHECK_SUCCESS_RATIO.value(Float.class);

        logger.debug(String.format(
                "[HA worker] host[uuid:%s] successRatio=%f, threshold=%f, result=%s, error=%s",
                hostUuid, ratio, threshold, (ratio >= threshold) ? "NoHA" : "HARequired",
                StringUtils.join(CollectionUtils.transform(errors, ErrorCode::getDetails), ", ")));

        if (ratio >= threshold) {
            details = i18n("the host[uuid:%s] is alive, no HA required. The host will be reconnected soon", hostUuid);
            result.evidence = details;
        } else {
            String errorDetails = StringUtils.join(CollectionUtils.transform(errors, ErrorCode::getDetails), ", ");
            details = i18n("the host[uuid:%s] is dead. (%s) The vm on this host start HA", hostUuid, errorDetails);
            result = HostScanResult.ofDead(details);
        }
        return result;
    }
}
