package org.zstack.header.tpm;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.rest.SDKPackage;
import org.zstack.header.search.SearchConstant;
import org.zstack.header.tpm.api.APIQueryTpmMsg;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.vm.VmInstanceVO;

@SDKPackage(packageName="org.zstack.sdk.tpm")
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "tpm";
    }

    {
        permissionBuilder()
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        resourceEnsembleContributorBuilder()
                .resource(TpmVO.class)
                .contributeTo(VmInstanceVO.class)
                .build();

        roleContributorBuilder()
                .actionsInThisPermission()
                .toOtherRole()
                .build();

        apis()
                .inPackage("org.zstack.header.tpm.api")
                .toService("tpm")
                .build();
        apis()
                .api(APIQueryTpmMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
