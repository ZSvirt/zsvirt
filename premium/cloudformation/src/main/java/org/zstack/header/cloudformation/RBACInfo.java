package org.zstack.header.cloudformation;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;
/**
 * Created by kayo on 2018/7/10.
 */
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "cloudformation";
    }

    {
        permissionBuilder()
                .targetResources(ResourceStackVO.class)
                .zsvAdvancedAvailable()
                .build();

        contributeNormalApiToOtherRole();

        globalReadableResourceBuilder()
                .resources(StackTemplateVO.class)
                .build();
        apis()
                .api(
                        APIAddStackTemplateMsg.class,
                        APICheckStackTemplateParametersMsg.class,
                        APICreateResourceStackMsg.class,
                        APIDecodeStackTemplateMsg.class,
                        APIDeleteResourceStackMsg.class,
                        APIDeleteStackTemplateMsg.class,
                        APIGetResourceFromResourceStackMsg.class,
                        APIGetResourceStackFromResourceMsg.class,
                        APIGetSupportedCloudFormationResourcesMsg.class,
                        APIPreviewResourceStackMsg.class,
                        APIRestartResourceStackMsg.class,
                        APIUpdateResourceStackMsg.class,
                        APIUpdateStackTemplateMsg.class
                )
                .toService("cloudformation")
                .build();

        apis()
                .api(
                        APIQueryEventFromResourceStackMsg.class,
                        APIQueryResourceStackMsg.class,
                        APIQueryStackTemplateMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

        apis()
                .inPackage("org.zstack.header.cloudformation.monitor")
                .toService("cloudformation")
                .build();
    }
}
