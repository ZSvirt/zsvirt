package org.zstack.softwarePackage;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.rest.SDKPackage;
import org.zstack.header.search.SearchConstant;
import org.zstack.softwarePackage.header.APIQuerySoftwarePackageMsg;

@SDKPackage(packageName = "org.zstack.sdk.softwarePackage")
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "software-package-plugin";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .zsvBasicAvailable()
                .build();

        apis()
                .inPackage("org.zstack.softwarePackage.header")
                .toService("softwarePackage")
                .build();

        apis()
                .api(APIQuerySoftwarePackageMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
