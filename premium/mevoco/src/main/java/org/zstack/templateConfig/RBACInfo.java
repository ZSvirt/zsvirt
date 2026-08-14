package org.zstack.templateConfig;
import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "configuration-template";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .zsvProAvailable()
                .build();
        apis()
                .inThisPackage()
                .toService("templateConfig")
                .build();

        apis()
                .api(
                        APIQueryGlobalConfigTemplateMsg.class,
                        APIQueryTemplateConfigMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
