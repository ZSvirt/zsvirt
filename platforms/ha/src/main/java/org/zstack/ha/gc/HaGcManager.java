package org.zstack.ha.gc;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.gc.GCStatus;
import org.zstack.core.gc.GarbageCollectorVO;
import org.zstack.core.gc.GarbageCollectorVO_;
import org.zstack.ha.HaHelper;
import org.zstack.ha.header.HaGlobalConfig;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmHaExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.i18n;

public class HaGcManager {
    private static final CLogger logger = Utils.getLogger(HaGcManager.class);

    @Autowired
    private PluginRegistry pluginRegistry;

    public void submitNeverStopVmGCJob(String vmUuid){
        String hostUuid = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.hostUuid)
                .findValue();
        if (!HaHelper.isSelfFencerDone(vmUuid, hostUuid)) {
            return;
        }
        for (VmHaExtensionPoint ext : pluginRegistry.getExtensionList(VmHaExtensionPoint.class)) {
            ErrorCode errorCode = ext.preHaStartVm(vmUuid);
            if (errorCode != null) {
                logger.warn(String.format("can not ha for vm[%s]: %s", vmUuid, errorCode.getReadableDetails()));
                return;
            }
        }

        setGCJobForOneVm(vmUuid);
    }

    private void setGCJobForOneVm(String vmUuid) {
        String gcName = NeverStopVmGC.getGCName(vmUuid);
        if (Q.New(GarbageCollectorVO.class)
                .eq(GarbageCollectorVO_.name, gcName)
                .notEq(GarbageCollectorVO_.status, GCStatus.Done)
                .isExists()) {
            logger.debug(String.format("There is already a NeverStopVmGC on Vm[uuid:%s], skip.", vmUuid));
            return;
        }

        VmInstanceVO vm = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .find();

        NeverStopVmGC gc = new NeverStopVmGC();
        gc.NAME = gcName;
        gc.vm = VmInstanceInventory.valueOf(vm);
        gc.submit(HaGlobalConfig.NEVER_STOP_VM_FAILURE_RETRY_DELAY.value(Long.class), TimeUnit.SECONDS);
        logger.debug(i18n("A GC job is submitted to HA the VM[retry delay: %s seconds]", HaGlobalConfig.NEVER_STOP_VM_FAILURE_RETRY_DELAY.value(Long.class)));
    }
}
