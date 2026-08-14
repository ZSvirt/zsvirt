package org.zstack.kvm;

import org.zstack.core.config.schema.GuestOsCharacter;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.vm.VmAfterAttachNicExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicSetDriverExtensionPoint;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.kvm.KVMHostFactory.allGuestOsCharacter;

public class KVMGuestOsCharacterExtensionPoint implements
        VmAfterAttachNicExtensionPoint,
        VmNicSetDriverExtensionPoint
{
    private static final CLogger logger = Utils.getLogger(KVMGuestOsCharacterExtensionPoint.class);

    private String getNicDriverFromConfig(VmInstanceInventory vm) {
        String vmArchPlatformRelease = String.format("%s_%s_%s", vm.getArchitecture(), vm.getPlatform(), vm.getGuestOsType());
        GuestOsCharacter.Config config = allGuestOsCharacter.get(vmArchPlatformRelease);
        if (config == null) {
            logger.warn(String.format("cannot find guest os character for vm[uuid:%s, arch:%s, platform:%s, guestOsType:%s]",
                    vm.getUuid(), vm.getArchitecture(), vm.getPlatform(), vm.getGuestOsType()));
            return null;
        }

        return config.getNicDriver();
    }

    @Override
    public void afterAttachNic(String nicUuid, VmInstanceInventory vmInstanceInventory, Completion completion) {
        if (!Q.New(VmNicVO.class)
                .eq(VmNicVO_.uuid, nicUuid)
                .isExists()) {
            logger.warn(String.format("cannot find nic[uuid:%s]", nicUuid));
            completion.success();
            return;
        }

        String driver = getNicDriverFromConfig(vmInstanceInventory);
        if (driver == null) {
            completion.success();
            return;
        }

        SQL.New(VmNicVO.class)
                .eq(VmNicVO_.uuid, nicUuid)
                .set(VmNicVO_.driverType, driver)
                .update();
        logger.debug(String.format("set nic[uuid:%s] driver type to %s", nicUuid, driver));
        completion.success();
    }

    @Override
    public void afterAttachNicRollback(String nicUuid, VmInstanceInventory vmInstanceInventory, NoErrorCompletion completion) {
        completion.done();
    }

    @Override
    public String getPreferredVmNicDriver(VmInstanceInventory vm) {
        return getNicDriverFromConfig(vm);
    }
}
