package org.zstack.ldap;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.rest.SDKPackage;
import org.zstack.header.search.SearchConstant;
import org.zstack.ldap.api.APIQueryLdapServerMsg;

@SDKPackage(packageName = "org.zstack.sdk.identity.ldap")
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "ldap";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .zsvProAvailable()
                .build();

        apis()
                .inPackage("org.zstack.ldap.api")
                .toService("ldap")
                .build();
        apis()
                .api(APIQueryLdapServerMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
