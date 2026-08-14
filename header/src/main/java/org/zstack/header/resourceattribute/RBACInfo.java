package org.zstack.header.resourceattribute;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.resourceattribute.api.APIQueryResourceAttributeKeyMsg;
import org.zstack.header.resourceattribute.api.APIQueryResourceAttributeValueMsg;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyVO;
import org.zstack.header.rest.SDKPackage;
import org.zstack.header.search.SearchConstant;

@SDKPackage(packageName="org.zstack.sdk.attribute")
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "attribute";
    }

    {
        permissionBuilder()
                .communityAvailable()
                .build();

        contributeNormalApiToOtherRole();

        globalReadableResourceBuilder()
                .resources(ResourceAttributeKeyVO.class)
                .build();

        attributeSupportResourceBuilder()
                .resources(ResourceAttributeKeyVO.class)
                .build();
    
        apis()
                .inPackage("org.zstack.header.resourceattribute.api")
                .toService("resourceAttribute")
                .build();
        apis()
                .api(
                        APIQueryResourceAttributeKeyMsg.class,
                        APIQueryResourceAttributeValueMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
