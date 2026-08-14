package org.zstack.pciDevice;

import org.zstack.header.description.PackageDescription;
import org.zstack.pciDevice.specification.mdev.APIUpdateMdevDeviceSpecMsg;
import org.zstack.pciDevice.specification.pci.APIUpdatePciDeviceSpecMsg;
import org.zstack.pciDevice.virtual.sr_iov.APIGenerateSriovPciDevicesMsg;
import org.zstack.pciDevice.virtual.sr_iov.APIUngenerateSriovPciDevicesMsg;
import org.zstack.pciDevice.virtual.vfio_mdev.APIGenerateMdevDevicesMsg;
import org.zstack.pciDevice.virtual.vfio_mdev.APIGetMdevDeviceCandidatesMsg;
import org.zstack.pciDevice.virtual.vfio_mdev.APIQueryMdevDeviceMsg;
import org.zstack.pciDevice.virtual.vfio_mdev.APIUngenerateMdevDevicesMsg;
import org.zstack.pciDevice.virtual.vfio_mdev.APIUpdateMdevDeviceMsg;

import org.zstack.header.search.SearchConstant;

import org.zstack.pciDevice.specification.mdev.APIQueryMdevDeviceSpecMsg;
import org.zstack.pciDevice.specification.mdev.APIQueryVmInstanceMdevDeviceSpecRefMsg;
import org.zstack.pciDevice.specification.pci.APIQueryPciDeviceSpecMsg;
import org.zstack.pciDevice.specification.pci.APIQueryVmInstancePciDeviceSpecRefMsg;
import org.zstack.pciDevice.virtual.sr_iov.APIQueryEthernetVFMsg;
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "pci-device";
    }

    {
        permissionBuilder()
                .adminOnlyAPIs(
                        APIGenerateSriovPciDevicesMsg.class,
                        APIUngenerateSriovPciDevicesMsg.class,
                        APIUpdatePciDeviceMsg.class,
                        APIDeletePciDeviceMsg.class,
                        APIUpdatePciDeviceSpecMsg.class,
                        APICreatePciDeviceOfferingMsg.class,
                        APIDeletePciDeviceOfferingMsg.class,
                        APIGenerateMdevDevicesMsg.class,
                        APIUngenerateMdevDevicesMsg.class,
                        APIUpdateMdevDeviceMsg.class,
                        APIUpdateMdevDeviceSpecMsg.class)
                .communityAvailable()
                .zsvProAvailable()
                .build();

        roleBuilder()
                .uuid("77affc9ac2eb452cb7170953460e9769")
                .permissionBaseOnThis()
                .build();

        roleContributorBuilder()
                .toOtherRole()
                .actions(
                    APIGetPciDeviceCandidatesForAttachingVmMsg.class,
                    APIQueryPciDeviceMsg.class,
                    APIQueryPciDeviceOfferingMsg.class,
                    APIQueryPciDevicePciDeviceOfferingMsg.class,
                    APIGetMdevDeviceCandidatesMsg.class,
                    APIQueryMdevDeviceMsg.class
                )
                .build();

        roleContributorBuilder()
                .roleName("legacy")
                .actionsInThisPermission()
                .build();
        apis()
                .api(
                        APIAttachPciDeviceToVmMsg.class,
                        APICreatePciDeviceOfferingMsg.class,
                        APIDeletePciDeviceMsg.class,
                        APIDeletePciDeviceOfferingMsg.class,
                        APIDetachPciDeviceFromVmMsg.class,
                        APIGetHostIommuStateMsg.class,
                        APIGetHostIommuStatusMsg.class,
                        APIGetPciDeviceCandidatesForAttachingVmMsg.class,
                        APIGetPciDeviceCandidatesForNewCreateVmMsg.class,
                        APIUpdateHostIommuStateMsg.class,
                        APIUpdatePciDeviceMsg.class
                )
                .toService("pciDevice")
                .build();

        apis()
                .api(
                        APIQueryPciDeviceMsg.class,
                        APIQueryPciDeviceOfferingMsg.class,
                        APIQueryPciDevicePciDeviceOfferingMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

        apis()
                .inPackage("org.zstack.pciDevice.gpu")
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.pciDevice.specification.mdev")
                .toService("pci.specification")
                .build();
        apis()
                .api(
                        APIQueryMdevDeviceSpecMsg.class,
                        APIQueryVmInstanceMdevDeviceSpecRefMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.pciDevice.specification.pci")
                .toService("pci.specification")
                .build();
        apis()
                .api(
                        APIQueryPciDeviceSpecMsg.class,
                        APIQueryVmInstancePciDeviceSpecRefMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.pciDevice.virtual.sr_iov")
                .toService("pciDevice")
                .build();
        apis()
                .api(APIQueryEthernetVFMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.pciDevice.virtual.vfio_mdev")
                .toService("mdevDevice")
                .build();
        apis()
                .api(
                        APIGenerateMdevDevicesMsg.class,
                        APIUngenerateMdevDevicesMsg.class
                )
                .toService("pciDevice")
                .build();
        apis()
                .api(APIQueryMdevDeviceMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
