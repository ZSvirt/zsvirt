package org.zstack.guesttools.pvpanic;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.vm.VmCrashHistoryVO;
import org.zstack.header.vm.VmCrashHistoryVO_;
import org.zstack.resourceconfig.ResourceConfigFacade;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Created by Wenhao.Zhang on 21/06/22
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmCrashRecorder {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceConfigFacade rcf;

    public void record(LocalDateTime crashTime, String vmUuid) {
        VmCrashHistoryVO vo = new VmCrashHistoryVO();
        vo.setUuid(vmUuid);
        vo.setDateInLong(crashTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        dbf.persist(vo);
    }

    public boolean hasReachedThreshold(LocalDateTime crashTime, String vmUuid) {
        int duration = getThresholdDuration(vmUuid);
        int times = getThresholdTimes(vmUuid);
        if (times == 0 || duration == 0) {
            return false;
        }
        long crashDateInLong = crashTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    
        long count = Q.New(VmCrashHistoryVO.class)
                .eq(VmCrashHistoryVO_.uuid, vmUuid)
                .lt(VmCrashHistoryVO_.dateInLong, crashDateInLong)
                .gte(VmCrashHistoryVO_.dateInLong, crashDateInLong - duration * 1000)
                .count();
        return count >= times;
    }

    private int getThresholdDuration(String vmUuid) {
        return rcf.getResourceConfigValue(VmGlobalConfig.VM_REBOOT_THRESHOLD_DURATION, vmUuid, Integer.class);
    }

    private int getThresholdTimes(String vmUuid) {
        return rcf.getResourceConfigValue(VmGlobalConfig.VM_REBOOT_THRESHOLD_TIMES, vmUuid, Integer.class);
    }
}
