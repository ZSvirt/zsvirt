package org.zstack.pluginpremium.externalapiadapter;

/**
 * Created by lining on 2018/4/20.
 */
public interface ExternalAPIAdapterConstants {

    String ALL_PATH = "/externalapi/**";

    String ZSTACK_API_APIID_KEY = "apiId";
    String ZSTACK_SESSIONID_KEY = "sessionId";
    String ZSTACK_RESOURCEUUID_KEY = "resourceUuid";

    int ECS_QUERY_API_PAGESIZE_DEFAULT_VALUE = 10;
    int ECS_QUERY_API_PAGENUMBER_DEFAULT_VALUE = 1;
    String ECS_QUERY_API_PAGENUMBER_KEY = "PageNumber";
    String ECS_QUERY_API_PAGESIZE_KEY = "PageSize";
    String ECS_API_ACTION_KEY = "Action";
    String ECS_API_REQUESTID_KEY = "RequestId";
    String ECS_API_SIGNATURE_KEY = "Signature";
    String ECS_API_ACCESSKEYID_KEY = "AccessKeyId";
    String ECS_API_REGIONID_KEY = "RegionId";
    String ECS_API_ZONEID_KEY = "ZoneId";
    String ECS_API_CLIENTTOKEN_KEY = "ClientToken";
    String ECS_UUID = "InstanceId";


    String ZSTACK_API_ZONEID_KEY = "clusterUuid";

    String ECS_API_TYPE_KEY = "Type";
    String ZSTACK_API_TYPE_KEY = "type";
    String ZSTACK_VM_NIC_UUIDS = "vmNicUuids";
    String ECS_API_STATUS_KEY = "Status";
    String ZSTACK_API_STATUS_KEY = "status";
    String ZSTACK_API_STATE_KEY = "state";
    String ECS_API_CREATION_TIME = "CreationTime";

    String ECS_API_PRIORITY = "Priority";

    String ECS_ADDRESS_TYPE_INTERNET = "internet";
    String ECS_ADDRESS_TYPE_INTRANET = "intranet";
    String ECS_NETWORK_TYPE_CLASSIC = "classic";
    String ECS_NETWORK_TYPE_VPC = "vpc";
    String ECS_NETWORK_IP_ADDRESS = "IpAddress";
    String ECS_NETWORK_BANDWIDTH = "Bandwidth";
    String ECS_EIP_ADDRESS = "EipAddress";
    String ECS_EIP_ALLOCATION_ID = "AllocationId";

    String ZSTACK_VIP_UUID = "vipUuid";

    String ZSTACK_QUERY_CONDITIONS_KEY = "conditions";
    String ZSTACK_QUERY_REPLYWITHCOUNT_KEY = "replyWithCount";
    String ZSTACK_QUERY_LIMIT_KEY = "limit";

    String ZSTACK_UUID = "uuid";
    String ZSTACK_NAME = "name";
    String ECS_INSTANCE = "Instance";
    String ECS_INSTANCES = "Instances";
    String ECS_INSTANCE_ID = "InstanceId";
    String ECS_INSTANCE_IDS = "InstanceIds";
    String ECS_INSTANCE_NAME = "InstanceName";
    String ZSTACK_VM_INSTANCE_UUID = "vmInstanceUuid";
    String ECS_INSTANCE_HOSTNAME = "HostName";
    String ECS_INSTANCE_PASSWORD = "Password";
    String ECS_INSTANCE_PRIVATE_IP = "PrivateIpAddress";
    String ECS_INSTANCE_PRIMARY_IP = "PrimaryIpAddress";
    String ZSTACK_NIC_IP = "ip";
    String ECS_INSTANCE_SYSTEM_DISK_SIZE = "SystemDisk.Size";
    String ZSTACK_INSTANCE_SYSTEM_DISK_SIZE = "rootDiskOfferingUuid";
    String ZSTACK_INSTANCE_DATA_DISK_SIZE = "dataDiskOfferingUuids";
    String ZSTACK_INSTANCE_L3NETWORKS = "l3NetworkUuids";
    String ZSTACK_INSTANCE_DEFAULT_NETWORK = "defaultL3NetworkUuid";
    String ZSTACK_INSTANCE_CREATE_STRATEGY = "strategy";

    String ECS_DEPLOYMENT_SET = "DeploymentSet";
    String ECS_DEPLOYMENT_SETS = "DeploymentSets";
    String ECS_DEPLOYMENT_SET_ID = "DeploymentSetId";
    String ECS_DEPLOYMENT_SET_IDS = "DeploymentSetIds";
    String ECS_DEPLOYMENT_SET_NAME = "DeploymentSetName";
    String ECS_DEPLOYMENT_SET_DESCRIPTION = "DeploymentSetDescription";
    String ECS_DEPLOYMENT_SET_STRATEGY = "Strategy";
    String ZSTACK_DEPLOYMENT_SET_STRATEGY = "policy";
    String ECS_DEPLOYMENT_SET_GRANULARITY = "Granularity";
    String ECS_DEPLOYMENT_SET_DOMAIN = "Domain";
    String ECS_DEPLOYMENT_SET_INSTANCE_AMOUNT = "InstanceAmount";
    String ZSTACK_DEPLOYMENT_SET_APPLIANCE = "appliance";

    String ZSTACK_API_DESCRIPTION_KEY = "description";
    String ECS_API_DESCRIPTION_KEY = "Description";

    String ECS_USERDATA = "UserData";
    String ZSTACK_SYSTEMTAGS = "systemTags";

    String ECS_TOTAL_COUNT = "TotalCount";

    String ECS_DISK_ID = "DiskId";
    String ZSTACK_DISK_UUID = "volumeUuid";
    String ECS_DISK = "Disk";
    String ECS_DISKS = "Disks";
    String ECS_DISK_IDS = "DiskIds";
    String ECS_DISK_NAME = "DiskName";
    String ECS_DISK_DELETEWITHINSTANCE = "DeleteWithInstance";
    String ECS_DISK_DELETEWITHINSTANCE_TAG = "deleteWithInstance::true";
    String ECS_API_SIZE = "Size";
    String ZSTACK_API_SIZE = "size";
    String ECS_DISK_NEW_SIZE = "NewSize";
    String ZSTACK_DISK_OFFERING_UUID = "diskOfferingUuid";
    String ZSTACK_DISK_SNAPSHOT_UUID = "volumeSnapshotUuid";
    String ECS_DISK_VIRTIO_SCSI = "VirtioSCSI";
    String ECS_DISK_TYPE = "DiskType";
    String ECS_DISK_PORTABLE = "Portable";
    String ECS_DISK_ENABLE_SHARED = "EnableShared";
    String ZSTACK_DISK_ENABLE_SHARED = "isShareable";
    String ECS_DISK_ENCRYPTED = "Encrypted";
    String ECS_DISK_CATEGORY = "Category";
    String ECS_DISK_DELETE_AUTO_SNAPSHOT = "DeleteAutoSnapshot";
    String ECS_DISK_ENABLE_AUTO_SNAPSHOT = "EnableAutoSnapshot";
    String ECS_DISK_DEVICE = "Device";
    String ECS_DISK_IOPS = "IOPS";
    String ZSTACK_DISK_INSTANCE_ID = "vmUuid";

    String ECS_VPC_VPC_ID = "VpcId";
    String ECS_VPC_VPC_NAME = "VpcName";
    String ECS_VPC_VROUTER_ID = "VRouterId";
    String ECS_VPC_VROUTER_NAME = "VRouterName";
    String ECS_VPC_VSWITCH_ID = "VSwitchId";
    String ECS_VPC_VSWITCH_NAME = "VSwitchName";

    String ECS_SNAPSHOT_ID = "SnapshotId";
    String ECS_SNAPSHOT_ENABLED_POLICY = "EnableAutomatedSnapshotPolicy";
    String ECS_SNAPSHOT_AUTO_POLICY_ID = "AutoSnapshotPolicyId";

    String ECS_INSTANCE_TYPE = "InstanceType";
    String ECS_INSTANCE_TYPE_ID = "InstanceTypeId";
    String ZSTACK_INSTANCE_TYPE_ID = "instanceOfferingUuid";
    String ECS_INSTANCE_TYPE_BANDWIDTH_INBOUND = "InstanceBandwidthRx";
    String ECS_INSTANCE_TYPE_BANDWIDTH_OUTBOUND = "InstanceBandwidthTx";
    String ECS_INSTANCE_TYPE_ENI_QUANTITY = "EniQuantity";
    String ECS_INSTANCE_TYPE_GPU_SPEC = "GPUSpec";
    String ECS_INSTANCE_TYPE_GPU_AMOUNT = "GPUAmount";
    String ZSTACK_PCI_DEVICE_SPEC_UUID_KEY = "pciSpecUuidKey";
    String ZSTACK_PCI_DEVICE_SPEC_AMOUNT_KEY = "pciSpecAmountKey";
    String ECS_INSTANCE_NETWORK_TYPE = "InstanceNetworkType";

    String ECS_IMAGE_ID = "ImageId";
    String ECS_IMAGE_NAME = "ImageName";
    String ZSTACK_IMAGE_ID = "imageUuid";
    String ZSTACK_IMAGE_SYSTEM = "system";
    String ECS_IMAGE_ARCHITECTURE = "Architecture";
    String ECS_IMAGE_PLATFORM = "Platform";

    String ECS_SECURITY_GROUP_ID = "SecurityGroupId";
    String ZSTACK_SECURITY_GROUP_ID = "securityGroupUuid";
    String ECS_SECURITY_GROUP_NAME = "SecurityGroupName";
    String ZSTACK_SECURITY_GROUP_RULES = "rules";
    String ECS_SECURITY_GROUP_RULE_INGRESS = "Ingress";
    String ECS_SECURITY_GROUP_RULE_EGRESS = "Egress";
    String ECS_SECURITY_GROUP_RULE_SOURCE_CIDR = "SourceCidrIp";
    String ECS_SECURITY_GROUP_RULE_DEST_CIDR = "DestCidrIp";
    String ECS_SECURITY_GROUP_SOURCE_GROUP_ID = "SourceGroupId";
    String ZSTACK_SECURITY_GROUP_SOURCE_GROUP_ID = "remoteSecurityGroupUuids";
    String ECS_SECURITY_GROUP_DEST_GROUP_ID = "DestGroupId";
    String ECS_SECURITY_GROUP_RULE_PROTOCOL = "IpProtocol";
    String ECS_SECURITY_GROUP_RULE_PORT_RANGE = "PortRange";
    String ECS_SECURITY_GROUP_NIC_TYPE = "NicType";

    String ECS_ROUTE_TABLE_ID = "RouteTableId";
    String ZSTACK_ROUTE_TABLE_ID = "routeTableUuid";

    String ECS_SLB_ID = "LoadBalancerId";
    String ZSTACK_SLB_ID = "loadBalancerUuid";
    String ECS_SLB_BACKEND_SERVERS = "BackendServers";
    String ECS_SLB_BACKEND_SERVER = "BackendServer";
    String ECS_SLB_BACKEND_SERVER_TYPE_ECS = "ecs";
    String ECS_SLB_BACKEND_SERVER_TYPE_ENI = "eni";
    String ECS_SLB_ADDRESS = "Address";
    String ECS_SLB_ADDRESS_TYPE = "AddressType";
    String ECS_SLB_NETWORK_TYPE = "NetworkType";

    String ECS_SLB_LISTENER_HEALTH_CHECK_TYPE = "HealthCheckType";
    String ZSTACK_SLB_LISTENER_HEALTH_CHECK_TYPE = "healthCheckProtocol";
    String ECS_SLB_LISTENER_HEALTH_CHECK_TYPE_HTTP = "http";
    String ECS_SLB_LISTENER_HEALTH_CHECK_TYPE_TCP = "tcp";
    String ECS_SLB_LISTENER_HEALTH_CHECK_URI = "HealthCheckURI";
    String ZSTACK_SLB_LISTENER_HEALTH_CHECK_URI = "healthCheckURI";
    String ECS_SLB_LISTENER_HEALTH_CHECK_HTTP_CODE = "HealthCheckHttpCode";
    String ZSTACK_SLB_LISTENER_HEALTH_CHECK_HTTP_CODE = "healthCheckHttpCode";
    String ZSTACK_SLB_LISTENER_HEALTH_CHECK_METHOD = "healthCheckMethod";
    String ZSTACK_SLB_LISTENER_HEALTH_CHECK_METHOD_HEAD = "HEAD";

    int ZSTACK_ASYNC_QUERY_COUNT = 2;
    int QUERY_INTERVAL_TIME = 1;
    int ZSTACK_ASYNC_API_DEFAULT_TIMEOUT = 5;
    String COMMON_SEPARATOR = ",";
    int ECS_SECURITY_GROUP_PORT_RANGE_OMIT_FLAG = -1;
    String ECS_STATE_PENDING = "Pending";

    interface ECSErrorCode {
        String InvalidParameter = "InvalidParameter";
        String InternalError = "InternalError";
        String ApiUnsupported = "global.errcode.api_unsupported";
        String ACTION_FAILED = "ZStackActionFailed";
    }

    interface SUPPORT_RESOURCE_INFO {
        String INSTANCE_TYPE_FAMILY = "ecs.s2";
        String INSTANCE_TYPE_GENERATION = "ecs-1";
        String DISK_CATEGORY = "cloud_efficiency";
    }

    interface SLB_QUOTA_CONST {
        String QUOTA_NAME = "QuotaName";
        String MAX = "Max";
        String COMMENT = "Comment";

        String SLB_PER_USER = "slbs-per-user";
        String SLB_PER_USER_DEF = "20";
        String SLB_PER_USER_COM = "Max number of SLB instances per account";

        String SERVER_CA_PER_REGION = "server-cers-per-region";
        String SERVER_CA_PER_REGION_DEF = "20";
        String SERVER_CA_PER_REGION_COM = "Max number of Server certificates per region";

        String CLIENT_CA_PER_REGION = "client-ca-cers-per-region";
        String CLIENT_CA_PER_REGION_DEF = "20";
        String CLIENT_CA_PER_REGION_COM = "Max number of Client CA certificates per region";

        String SLB_PER_SERVER = "slbs-per-backendserver";
        String SLB_PER_SERVER_DEF = "20";
        String SLB_PER_SERVER_COM = "Max number of SLB instances that a single backend server can be attached to";

        String SERVER_PER_SLB = "backendservers-per-slb";
        String SERVER_PER_SLB_DEF = "20";
        String SERVER_PER_SLB_COM = "Max number of backend servers can be attached to a single SLB instance";

        String LISTENER_PER_SLB = "listeners-per-slb";
        String LISTENER_PER_SLB_DEF = "20";
        String LISTENER_PER_SLB_COM = "Max number of listeners per SLB instance";

        String RULE_PER_LISTENER = "rules-per-listener";
        String RULE_PER_LISTENER_DEF = "20";
        String RULE_PER_LISTENER_COM = "Max number of forwarding rule per listener";

        String DOMAIN_EXT_PER_LISTENER = "domain-extensions-per-listener";
        String DOMAIN_EXT_PER_LISTENER_DEF = "20";
        String DOMAIN_EXT_PER_LISTENER_COM = "Max number of extension domains per listener";

        String ACL_PER_REGION = "acls-per-region";
        String ACL_PER_REGION_DEF = "20";
        String ACL_PER_REGION_COM = "Max number of access control lists per region";

        String LISTENER_PER_ACL = "listeners-per-acl";
        String LISTENER_PER_ACL_DEF = "20";
        String LISTENER_PER_ACL_COM = "Max number of listeners that an single access control list can be attached to";

        String ENTRY_PER_ACL = "entries-per-acl";
        String ENTRY_PER_ACL_DEF = "20";
        String ENTRY_PER_ACL_COM = "Max number of entries per access control list";
    }

    interface VPC_CLOUD_RESOURCE_CONST {
        String CLOUD_RES = "CloudResources";
        String CLOUD_RES_SET_TYPE = "CloudResourceSetType";
        String RES_TYPE = "ResourceType";
        String RES_COUNT = "ResourceCount";
    }
}
