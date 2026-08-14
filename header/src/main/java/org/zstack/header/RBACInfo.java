package org.zstack.header;

import org.zstack.header.core.APIGetChainTaskMsg;
import org.zstack.header.description.PackageDescription;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "core-open-source";
    }

    {
        permissionBuilder()
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleBuilder()
                .name("other")
                .uuid(AccountConstant.OTHER_ROLE_UUID)
                .actions(APIIsOpensourceVersionMsg.class)
                .build();

        roleBuilder()
                .name("legacy")
                .uuid(AccountConstant.LEGACY_ROLE_UUID)
                .actions("org.zstack.header.**")
                .build();

        roleBuilder()
                .name("resource-viewer")
                .uuid(AccountConstant.ALL_RESOURCES_READABLE_ROLE_UUID)
                .build();

        roleBuilder()
                .name("sod-system-administrator")
                .uuid(AccountConstant.SOD_SYSTEM_ADMIN_ROLE_UUID)
                .build();

        roleBuilder()
                .name("sod-security-administrator")
                .uuid(AccountConstant.SOD_SECURITY_ADMIN_ROLE_UUID)
                .build();

        roleBuilder()
                .name("sod-auditor")
                .uuid(AccountConstant.SOD_AUDITOR_ROLE_UUID)
                .build();
        // Note: In enterprise edition, APIIsOpensourceVersionMsg.serviceId will be rewritten
        //       by END position interceptor to routing to a new service
        apis()
                .api(APIIsOpensourceVersionMsg.class)
                .toService(AccountConstant.SERVICE_ID)
                .build();

        apis()
                .api(APIGetChainTaskMsg.class)
                .toService("core")
                .build();

        apis()
                .inPackage("org.zstack.header.core.encrypt")
                .toService("encrypt")
                .build();

        apis()
                .inPackage("org.zstack.header.query")
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
