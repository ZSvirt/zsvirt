package org.zstack.network.service.flat;

/**
 * Created by Qi Le on 2019/9/9
 */
public interface IpStatisticConstants {
    interface ResourceType {
        String ALL = "All";
        String VM = "VM";
        String VIP = "Vip";
        String OTHER = "Other";
        String VROUTER = "VRouter";
        String VPC_VROUTER = "VpcVRouter";
        String DHCP = "DHCP";
        String KERNEL_INTERFACE = "KernelInterface";
    }

    interface SortBy {
        String IP = "Ip";
        String CREATE_TIME = "CreateDate";
    }

    interface SortDirection {
        String ASC = "asc";
        String DESC = "desc";
    }

    interface VmType {
        String USER_VM = "UserVm";
        String APPLIANCE_VM = "ApplianceVm";
        String VROUTER = "vrouter";
        String VPC_VROUTER = "vpcvrouter";
    }
}
