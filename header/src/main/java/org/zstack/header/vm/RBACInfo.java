package org.zstack.header.vm;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;
import org.zstack.header.vm.cdrom.APIQueryVmCdRomMsg;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.volume.APICreateDataVolumeFromVolumeTemplateMsg;
import org.zstack.header.volume.APICreateDataVolumeMsg;

import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.isEmpty;
import static org.zstack.utils.CollectionUtils.transform;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "vm";
    }

    {
        permissionBuilder()
                .targetResources(VmInstanceVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        expandedPermission(APICreateVmInstanceMsg.class, api -> {
            if (!isEmpty(api.getDataDiskOfferingUuids())) {
                return transform(api.getDataDiskOfferingUuids(), uuid -> {
                    APICreateDataVolumeMsg expendMsg = new APICreateDataVolumeMsg();
                    expendMsg.setDiskOfferingUuid(uuid);
                    return expendMsg;
                });
            }

            if (!isEmpty(api.getDataDiskSizes())) {
                return transform(api.getDataDiskSizes(), size -> {
                    APICreateDataVolumeMsg expendMsg = new APICreateDataVolumeMsg();
                    expendMsg.setDiskSize(size);
                    return expendMsg;
                });
            }

            if (!isEmpty(api.getDiskAOs()) && api.getDiskAOs().size() > 1) {
                return api.getDiskAOs().subList(1, api.getDiskAOs().size()).stream()
                        .map(diskAO -> {
                            if (diskAO.getSize() > 0) {
                                APICreateDataVolumeMsg expendMsg = new APICreateDataVolumeMsg();
                                expendMsg.setDiskSize(diskAO.getSize());
                                return expendMsg;
                            } else if (diskAO.getDiskOfferingUuid() != null) {
                                APICreateDataVolumeMsg expendMsg = new APICreateDataVolumeMsg();
                                expendMsg.setDiskOfferingUuid(diskAO.getDiskOfferingUuid());
                                return expendMsg;
                            } else if (diskAO.getTemplateUuid() != null) {
                                APICreateDataVolumeFromVolumeTemplateMsg expendMsg = new APICreateDataVolumeFromVolumeTemplateMsg();
                                expendMsg.setImageUuid(diskAO.getTemplateUuid());
                                return expendMsg;
                            }

                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }

            return Collections.emptyList();
        });

        resourceEnsembleContributorBuilder()
                .resource(VmNicVO.class)
                .resource(VmCdRomVO.class)
                .contributeTo(VmInstanceVO.class)
                .build();

        roleContributorBuilder()
                .actions(
                        APIQueryVmInstanceMsg.class,
                        APIGetVmBootOrderMsg.class,
                        APIGetVmCapabilitiesMsg.class,
                        APIGetVmHostnameMsg.class,
                        APIGetVmsCapabilitiesMsg.class,
                        APIQueryVmNicMsg.class,
                        APITakeVmConsoleScreenshotMsg.class,
                        APIGetVmConsoleAddressMsg.class
                )
                .toOtherRole()
                .build();

        roleBuilder()
                .uuid("5f93cf6444ec44cc83209744c8c3d7cc")
                .permissionBaseOnThis()
                .build();

        roleBuilder()
                .uuid("d6b79564f9b641a4b8bb85ea249151c2")
                .name("vm-operation-without-create-permission")
                .permissionBaseOnThis()
                .excludeActions(APICreateVmInstanceMsg.class)
                .build();

        attributeSupportResourceBuilder()
                .resources(VmInstanceVO.class)
                .build();
        apis()
                .api(
                        APIGetVmTaskMsg.class
                )
                .toService("core")
                .build();

        apis()
                .api(
                        APIQueryTemplatedVmInstanceMsg.class,
                        APIQueryVmInstanceMsg.class,
                        APIQueryVmNicMsg.class,
                        APIQueryVmPriorityConfigMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

        apis()
                .api(
                        APIGetMemorySnapshotGroupReferenceMsg.class
                )
                .toService("snapshot.volume")
                .build();

        apis()
                .api(
                        APIAttachIsoToVmInstanceMsg.class,
                        APIAttachL3NetworkToVmMsg.class,
                        APIAttachL3NetworkToVmNicMsg.class,
                        APIAttachVmNicToVmMsg.class,
                        APIChangeInstanceOfferingMsg.class,
                        APIChangeVmNicNetworkMsg.class,
                        APIChangeVmNicStateMsg.class,
                        APICleanupVmInstanceMetadataMsg.class,
                        APIConvertTemplatedVmInstanceToVmInstanceMsg.class,
                        APIConvertVmInstanceToTemplatedVmInstanceMsg.class,
                        APICreateVmInstanceFromVolumeMsg.class,
                        APICreateVmInstanceFromVolumeSnapshotGroupMsg.class,
                        APICreateVmInstanceFromVolumeSnapshotMsg.class,
                        APICreateVmInstanceMsg.class,
                        APICreateVmNicMsg.class,
                        APIDeleteTemplatedVmInstanceMsg.class,
                        APIDeleteVmBootModeMsg.class,
                        APIDeleteVmConsolePasswordMsg.class,
                        APIDeleteVmHostnameMsg.class,
                        APIDeleteVmNicMsg.class,
                        APIDeleteVmSshKeyMsg.class,
                        APIDeleteVmStaticIpMsg.class,
                        APIDestroyVmInstanceMsg.class,
                        APIDetachIsoFromVmInstanceMsg.class,
                        APIDetachL3NetworkFromVmMsg.class,
                        APIExpungeVmInstanceMsg.class,
                        APIFlattenVmInstanceMsg.class,
                        APIFstrimVmMsg.class,
                        APIGetCandidateIsoForAttachingVmMsg.class,
                        APIGetCandidateL3NetworksForChangeVmNicNetworkMsg.class,
                        APIGetCandidatePrimaryStoragesForCreatingVmMsg.class,
                        APIGetCandidateVmForAttachingIsoMsg.class,
                        APIGetCandidateZonesClustersHostsForCreatingVmMsg.class,
                        APIGetInterdependentL3NetworksBackupStoragesMsg.class,
                        APIGetInterdependentL3NetworksImagesMsg.class,
                        APIGetSpiceCertificatesMsg.class,
                        APIGetVmAttachableDataVolumeMsg.class,
                        APIGetVmAttachableL3NetworkMsg.class,
                        APIGetVmBootOrderMsg.class,
                        APIGetVmCapabilitiesMsg.class,
                        APIGetVmConsoleAddressMsg.class,
                        APIGetVmConsolePasswordMsg.class,
                        APIGetVmDeviceAddressMsg.class,
                        APIGetVmDnsMsg.class,
                        APIGetVmHostnameMsg.class,
                        APIGetVmInstanceMetadataFromPrimaryStorageMsg.class,
                        APIGetVmMigrationCandidateHostsMsg.class,
                        APIGetVmNicAttachedNetworkServiceMsg.class,
                        APIGetVmSshKeyMsg.class,
                        APIGetVmStartingCandidateClustersHostsMsg.class,
                        APIGetVmUptimeMsg.class,
                        APIGetVmsCapabilitiesMsg.class,
                        APIMigrateVmMsg.class,
                        APIPauseVmInstanceMsg.class,
                        APIRebootVmInstanceMsg.class,
                        APIRecoverVmInstanceMsg.class,
                        APIRegisterVmInstanceFromMetadataMsg.class,
                        APIReimageVmInstanceMsg.class,
                        APIResumeVmInstanceMsg.class,
                        APISetVmBootModeMsg.class,
                        APISetVmBootOrderMsg.class,
                        APISetVmBootVolumeMsg.class,
                        APISetVmClockTrackMsg.class,
                        APISetVmConsolePasswordMsg.class,
                        APISetVmDnsMsg.class,
                        APISetVmHostnameMsg.class,
                        APISetVmQxlMemoryMsg.class,
                        APISetVmSoundTypeMsg.class,
                        APISetVmSshKeyMsg.class,
                        APISetVmStaticIpMsg.class,
                        APIStartVmInstanceMsg.class,
                        APIStopVmInstanceMsg.class,
                        APITakeVmConsoleScreenshotMsg.class,
                        APIUpdatePriorityConfigMsg.class,
                        APIUpdateTemplatedVmInstanceMsg.class,
                        APIUpdateVmInstanceMetadataMsg.class,
                        APIUpdateVmInstanceMsg.class,
                        APIUpdateVmNicDriverMsg.class,
                        APIUpdateVmPriorityMsg.class
                )
                .toService("vmInstance")
                .build();

        apis()
                .inPackage("org.zstack.header.vm.cdrom")
                .toService("vmInstance")
                .build();
        apis()
                .api(APIQueryVmCdRomMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.header.vm.devices")
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
