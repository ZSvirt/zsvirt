package org.zstack.header.sshkeypair;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "ssh-key-pair";
    }

    {
        permissionBuilder()
                .targetResources(SshKeyPairVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        contributeNormalApiToOtherRole();
        apis()
                .inThisPackage()
                .toService("sshKeyPair")
                .build();

        apis()
                .api(
                        APIQuerySshKeyPairMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
