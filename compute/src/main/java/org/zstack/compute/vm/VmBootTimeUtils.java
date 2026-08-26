package org.zstack.compute.vm;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class VmBootTimeUtils {
    public static final String STOPPED_UPTIME = "0";
    public static final String UNKNOWN_UPTIME = "";
    private static final DateTimeFormatter BOOT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static boolean isBootTimeValidState(VmInstanceState state) {
        return state != null && state != VmInstanceState.Stopped && state != VmInstanceState.Destroyed;
    }

    public String getBootTime(String vmUuid) {
        if (StringUtils.isBlank(vmUuid)) {
            return null;
        }

        Timestamp bootTime = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.bootTime)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .findValue();
        return formatBootTime(bootTime);
    }

    public void resetBootTime(String vmUuid) {
        if (StringUtils.isBlank(vmUuid)) {
            return;
        }

        SQL.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .set(VmInstanceVO_.bootTime, Timestamp.valueOf(LocalDateTime.now()))
                .update();
    }

    public void clearBootTime(String vmUuid) {
        if (StringUtils.isBlank(vmUuid)) {
            return;
        }

        SQL.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .set(VmInstanceVO_.bootTime, (Timestamp) null)
                .update();
    }

    public void backfillBootTimeIfMissing(String vmUuid, String hostUptime) {
        Timestamp bootTime = parseBootTime(hostUptime);
        if (StringUtils.isBlank(vmUuid) || bootTime == null) {
            return;
        }

        SQL.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .isNull(VmInstanceVO_.bootTime)
                .set(VmInstanceVO_.bootTime, bootTime)
                .update();
    }

    private static String formatBootTime(Timestamp bootTime) {
        if (bootTime == null) {
            return null;
        }

        return bootTime.toLocalDateTime().format(BOOT_TIME_FORMATTER);
    }

    private static Timestamp parseBootTime(String bootTime) {
        if (StringUtils.isBlank(bootTime)) {
            return null;
        }

        try {
            return Timestamp.valueOf(LocalDateTime.parse(bootTime, BOOT_TIME_FORMATTER));
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}