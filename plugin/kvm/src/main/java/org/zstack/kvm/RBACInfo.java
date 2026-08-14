package org.zstack.kvm;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;
import org.zstack.kvm.hypervisor.message.APIQueryHostOsCategoryMsg;
import org.zstack.kvm.hypervisor.message.APIQueryKvmHypervisorInfoMsg;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "kvm-host";
    }

    {
        permissionBuilder()
                .normalAPIs(APIQueryHostOsCategoryMsg.class, APIQueryKvmHypervisorInfoMsg.class)
                .adminOnlyForAll()
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        contributeNormalApiToOtherRole();
        apis()
                .api(
                        APIAddKVMHostMsg.class,
                        APIUpdateKVMHostMsg.class
                )
                .toService("host")
                .build();

        apis()
                .api(
                        APIKvmRunShellMsg.class
                )
                .toService("kvm")
                .build();

        apis()
                .inPackage("org.zstack.kvm.hypervisor.message")
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
