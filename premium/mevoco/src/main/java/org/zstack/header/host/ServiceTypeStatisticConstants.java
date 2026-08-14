package org.zstack.header.host;


public interface ServiceTypeStatisticConstants {
    interface InterfaceType {
        String ALL = "All";
        String INTERFACE = "Interface";
        String BONDING = "Bonding";
    }

    interface ServiceType {
        String ALL = "All";
        String MANAGEMENT_NETWORK = "ManagementNetwork";
        String TENANT_NETWORK = "TenantNetwork";
        String STORAGE_NETWORK = "StorageNetwork";
        String BACKUP_NETWORK = "BackupNetwork";
        String MIGRATION_NETWORK = "MigrationNetwork";
    }

    interface SortBy {
        String INTERFACE_NAME = "InterfaceName";
        String VLAN_ID = "VlanId";
        String HOST_IP = "HostIp";
        String HOST_NAME = "HostName";
        String CLUSTER_NAME = "ClusterName";
        String CREATE_DATE = "CreateDate";
    }

    interface SortDirection {
        String ASC = "asc";
        String DESC = "desc";
    }
}
