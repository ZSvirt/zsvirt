package org.zstack.zmigrate.client;

import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;

import static org.zstack.core.Platform.err;
import static org.zstack.zmigrate.compute.ZMigrateUtils.findZMigrateVmUuid;
import static org.zstack.zmigrate.ZMigratePluginErrors.ZMIGRATE_GATEWAY_ERROR;
import static org.zstack.zmigrate.ZMigrateSystemTags.ZMIGRATE_MANAGEMENT;

public class ZMigrateGatewayHelper {

    public static class GatewayInfo {
        public final String vmUuid;
        public final String managementIp;
        public final String zoneUuid;

        public GatewayInfo(String vmUuid, String managementIp, String zoneUuid) {
            this.vmUuid = vmUuid;
            this.managementIp = managementIp;
            this.zoneUuid = zoneUuid;
        }
    }

    public static ErrorableValue<String> getGatewayManagementIp() {
        ErrorableValue<GatewayInfo> info = getGatewayInfo();
        if (!info.isSuccess()) {
            return ErrorableValue.ofErrorCode(info.error);
        }
        return ErrorableValue.of(info.result.managementIp);
    }

    public static ErrorableValue<GatewayInfo> getGatewayInfo() {
        String managementVmUuid = findZMigrateVmUuid(ZMIGRATE_MANAGEMENT);
        if (managementVmUuid == null) {
            return err(ZMIGRATE_GATEWAY_ERROR, "no ZMigrate management VM found").toErrorableValue();
        }

        String zoneUuid = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, managementVmUuid)
                .select(VmInstanceVO_.zoneUuid).findValue();
        if (zoneUuid == null) {
            return err(ZMIGRATE_GATEWAY_ERROR, "ZMigrate management VM not found")
                    .withOpaque("vm.uuid", managementVmUuid)
                    .toErrorableValue();
        }

        // deviceId=0 is the management NIC by convention for ZMigrate gateway VMs
        String managementIp = Q.New(VmNicVO.class)
                .eq(VmNicVO_.vmInstanceUuid, managementVmUuid)
                .eq(VmNicVO_.deviceId, 0)
                .select(VmNicVO_.ip).findValue();
        if (managementIp == null) {
            return err(ZMIGRATE_GATEWAY_ERROR, "no nic found on ZMigrate management VM")
                    .withOpaque("vm.uuid", managementVmUuid)
                    .toErrorableValue();
        }

        return ErrorableValue.of(new GatewayInfo(managementVmUuid, managementIp, zoneUuid));
    }
}
