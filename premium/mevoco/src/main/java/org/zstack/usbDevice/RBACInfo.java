package org.zstack.usbDevice;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "usb-device";
    }

    {
        permissionBuilder()
                .targetResources(UsbDeviceVO.class)
                .adminOnlyForAll()
                .normalAPIs(APIQueryUsbDeviceMsg.class)
                .normalAPIs(APIAttachUsbDeviceToVmMsg.class)
                .normalAPIs(APIDetachUsbDeviceFromVmMsg.class)
                .normalAPIs(APIGetUsbDeviceCandidatesForAttachingVmMsg.class)
                .communityAvailable()
                .zsvProAvailable()
                .build();

        roleContributorBuilder()
                .roleName("vm")
                .actionsInThisPermission()
                .build();

        roleContributorBuilder()
                .toOtherRole()
                .actions(
                    APIGetUsbDeviceCandidatesForAttachingVmMsg.class,
                    APIQueryUsbDeviceMsg.class
                )
                .build();

        roleContributorBuilder()
                .roleName("legacy")
                .actionsInThisPermission()
                .build();
        apis()
                .inThisPackage()
                .toService("usbDevice")
                .build();

        apis()
                .api(
                        APIQueryUsbDeviceMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
