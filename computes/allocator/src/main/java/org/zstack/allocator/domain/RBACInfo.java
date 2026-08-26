package org.zstack.allocator.domain;

import org.zstack.allocator.domain.api.APIGetAllocatorDomainCandidatesMsg;
import org.zstack.allocator.domain.api.APIGetAllocatorDomainVmsMsg;
import org.zstack.allocator.domain.api.APIQueryAllocatorDomainMsg;
import org.zstack.allocator.domain.entity.AllocatorDomainVO;
import org.zstack.allocator.domain.header.AllocatorDomainConstant;
import org.zstack.header.description.PackageDescription;
import org.zstack.header.rest.SDKPackage;
import org.zstack.header.search.SearchConstant;

@SDKPackage(packageName = "org.zstack.sdk.allocator.domain")
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "allocator.domain";
    }

    {
        permissionBuilder()
                .targetResources(AllocatorDomainVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleBuilder()
                .uuid("77affcd91a8f4e3b9c6d0a5f1e8b4723")
                .permissionBaseOnThis()
                .build();

        roleContributorBuilder()
                .actions(
                        APIQueryAllocatorDomainMsg.class,
                        APIGetAllocatorDomainVmsMsg.class,
                        APIGetAllocatorDomainCandidatesMsg.class
                )
                .toOtherRole()
                .build();

        apis()
                .inPackage("org.zstack.allocator.domain.api")
                .toService(AllocatorDomainConstant.SERVICE_ID)
                .build();

        apis()
                .api(APIQueryAllocatorDomainMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
