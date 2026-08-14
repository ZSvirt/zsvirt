package org.zstack.mevoco;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

import org.zstack.header.cluster.APICreateMiniClusterMsg;
import org.zstack.header.managementnode.APIGetFactoryModeStateMsg;
import org.zstack.header.managementnode.APIGetPlatformTimeZoneMsg;
import org.zstack.header.managementnode.APIUpdateFactoryModeStateMsg;
import org.zstack.header.storage.snapshot.APICreateVolumesSnapshotMsg;
import org.zstack.header.vm.APIChangeVmImageMsg;
import org.zstack.header.vm.APIChangeVmPasswordMsg;
import org.zstack.header.vm.APICloneVmInstanceMsg;
import org.zstack.header.vm.APICreateTemplatedVmInstanceFromVmInstanceMsg;
import org.zstack.header.vm.APICreateVmInstanceFromTemplatedVmInstanceMsg;
import org.zstack.header.vm.APIDeleteNicQosMsg;
import org.zstack.header.vm.APIDeleteVmUserDefinedXmlHookScriptMsg;
import org.zstack.header.vm.APIDeleteVmUserDefinedXmlMsg;
import org.zstack.header.vm.APIGetImageCandidatesForVmToChangeMsg;
import org.zstack.header.vm.APIGetNicQosMsg;
import org.zstack.header.vm.APIGetVirtualizerInfoMsg;
import org.zstack.header.vm.APIGetVmEmulatorPinningMsg;
import org.zstack.header.vm.APIGetVmInstanceFirstBootDeviceMsg;
import org.zstack.header.vm.APIGetVmMonitorNumberMsg;
import org.zstack.header.vm.APIGetVmNumaMsg;
import org.zstack.header.vm.APIGetVmQgaMsg;
import org.zstack.header.vm.APIGetVmRDPMsg;
import org.zstack.header.vm.APIGetVmUsbRedirectMsg;
import org.zstack.header.vm.APIGetVmXmlHookScriptMsg;
import org.zstack.header.vm.APIGetVmXmlMsg;
import org.zstack.header.vm.APIGetVmvNUMATopologyMsg;
import org.zstack.header.vm.APIQueryVmSchedHistoryMsg;
import org.zstack.header.vm.APISetNicQosMsg;
import org.zstack.header.vm.APISetVmCleanTrafficMsg;
import org.zstack.header.vm.APISetVmConsoleModeMsg;
import org.zstack.header.vm.APISetVmEmulatorPinningMsg;
import org.zstack.header.vm.APISetVmMonitorNumberMsg;
import org.zstack.header.vm.APISetVmNumaMsg;
import org.zstack.header.vm.APISetVmQgaMsg;
import org.zstack.header.vm.APISetVmRDPMsg;
import org.zstack.header.vm.APISetVmSecurityLevelMsg;
import org.zstack.header.vm.APISetVmUsbRedirectMsg;
import org.zstack.header.vm.APISetVmUserDefinedXmlHookScriptMsg;
import org.zstack.header.vm.APISetVmUserDefinedXmlMsg;
import org.zstack.header.vm.APISyncVmClockMsg;
import org.zstack.header.vm.APIUpdateVmNicMacMsg;
import org.zstack.header.volume.APIDeleteVolumeQosMsg;
import org.zstack.header.volume.APIGetVolumeIoThreadPinMsg;
import org.zstack.header.volume.APIGetVolumeQosMsg;
import org.zstack.header.volume.APIResizeDataVolumeMsg;
import org.zstack.header.volume.APIResizeRootVolumeMsg;
import org.zstack.header.volume.APISetVolumeIoThreadPinMsg;
import org.zstack.header.volume.APISetVolumeQosMsg;
import org.zstack.header.volume.APIValidateVolumeSnapshotChainMsg;
import org.zstack.header.volume.block.APIQueryBlockVolumeMsg;
import org.zstack.header.volume.block.APIQueryExponBlockVolumeMsg;
import org.zstack.header.volume.block.APIQueryXskyBlockVolumeMsg;
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "shareable-volume";
    }

    {
        permissionBuilder()
                .communityAvailable()
                .zsvProAvailable()
                .build();

        roleContributorBuilder()
                .roleName("volume")
                .actionsInThisPermission()
                .build();
        apis()
                .inThisPackage()
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

        apis()
                .api(APICreateMiniClusterMsg.class)
                .toService("mevoco")
                .build();
        apis()
                .api(
                        APIGetFactoryModeStateMsg.class,
                        APIGetPlatformTimeZoneMsg.class,
                        APIUpdateFactoryModeStateMsg.class
                )
                .toService("mevoco")
                .build();
        apis()
                .api(APICreateVolumesSnapshotMsg.class)
                .toService("mevoco")
                .build();
        apis()
                .api(
                        APIChangeVmImageMsg.class,
                        APIChangeVmPasswordMsg.class,
                        APICloneVmInstanceMsg.class,
                        APICreateTemplatedVmInstanceFromVmInstanceMsg.class,
                        APICreateVmInstanceFromTemplatedVmInstanceMsg.class,
                        APIDeleteNicQosMsg.class,
                        APIDeleteVmUserDefinedXmlHookScriptMsg.class,
                        APIDeleteVmUserDefinedXmlMsg.class,
                        APIGetImageCandidatesForVmToChangeMsg.class,
                        APIGetNicQosMsg.class,
                        APIGetVirtualizerInfoMsg.class,
                        APIGetVmEmulatorPinningMsg.class,
                        APIGetVmInstanceFirstBootDeviceMsg.class,
                        APIGetVmMonitorNumberMsg.class,
                        APIGetVmNumaMsg.class,
                        APIGetVmQgaMsg.class,
                        APIGetVmRDPMsg.class,
                        APIGetVmUsbRedirectMsg.class,
                        APIGetVmXmlHookScriptMsg.class,
                        APIGetVmXmlMsg.class,
                        APIGetVmvNUMATopologyMsg.class,
                        APISetNicQosMsg.class,
                        APISetVmCleanTrafficMsg.class,
                        APISetVmConsoleModeMsg.class,
                        APISetVmEmulatorPinningMsg.class,
                        APISetVmMonitorNumberMsg.class,
                        APISetVmNumaMsg.class,
                        APISetVmQgaMsg.class,
                        APISetVmRDPMsg.class,
                        APISetVmSecurityLevelMsg.class,
                        APISetVmUsbRedirectMsg.class,
                        APISetVmUserDefinedXmlHookScriptMsg.class,
                        APISetVmUserDefinedXmlMsg.class,
                        APISyncVmClockMsg.class,
                        APIUpdateVmNicMacMsg.class
                )
                .toService("mevoco")
                .build();
        apis()
                .api(APIQueryVmSchedHistoryMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .api(
                        APIDeleteVolumeQosMsg.class,
                        APIGetVolumeIoThreadPinMsg.class,
                        APIGetVolumeQosMsg.class,
                        APIResizeDataVolumeMsg.class,
                        APIResizeRootVolumeMsg.class,
                        APISetVolumeIoThreadPinMsg.class,
                        APISetVolumeQosMsg.class,
                        APIValidateVolumeSnapshotChainMsg.class
                )
                .toService("mevoco")
                .build();
        apis()
                .inPackage("org.zstack.header.volume.block")
                .toService("mevoco")
                .build();
        apis()
                .api(
                        APIQueryBlockVolumeMsg.class,
                        APIQueryExponBlockVolumeMsg.class,
                        APIQueryXskyBlockVolumeMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
