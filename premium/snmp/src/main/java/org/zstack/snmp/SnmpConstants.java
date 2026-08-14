package org.zstack.snmp;

/**
 * @Author : jingwang
 * @create 2023/7/13 10:00 AM
 */
public interface SnmpConstants {
    String ACTION_CATEGORY = "SNMP";
    String SERVICE_ID = "SNMP";
    String SNMP_AGENT_SIGNATURE = "snmp-agent-signature";

    String DEFAULT_SNMP_SECURITY_NAME = "cloud";
    String DEFAULT_SNMP_V1_V2_GROUP_NAME = "v1v2group";
    String DEFAULT_SNMP_V3_GROUP_NOAUTH_NOPRIV_NAME = "v3group_noAuthNoPriv";
    String DEFAULT_SNMP_V3_GROUP_AUTH_NOPRIV_NAME = "v3group_authNoPriv";
    String DEFAULT_SNMP_V3_GROUP_AUTH_PRIV_NAME = "v3group_authPriv";
    String DEFAULT_SNMP_FULL_READ_VIEW_NAME = "fullReadView";
    String DEFAULT_SNMP_FULL_READ_VIEW_OID = "1.3";

    String CLOUD_SNMP_KVM_HOST_OID = "1.3.6.1.4.1.60687.1.1.101";
    String CLOUD_SNMP_KVM_HOST_ENTRY_OID = "1.3.6.1.4.1.60687.1.1.101.101.1";

    String CLOUD_SNMP_PRIMARY_STORAGE_OID = "1.3.6.1.4.1.60687.1.1.102";
    String CLOUD_SNMP_PRIMARY_STORAGE_ENTRY_OID = "1.3.6.1.4.1.60687.1.1.102.1.1";

    String CLOUD_SNMP_BACKUP_STORAGE_OID = "1.3.6.1.4.1.60687.1.1.103";
    String CLOUD_SNMP_BACKUP_STORAGE_ENTRY_OID = "1.3.6.1.4.1.60687.1.1.103.1.1";

    String CLOUD_SNMP_VM_OID = "1.3.6.1.4.1.60687.1.1.104";
    String CLOUD_SNMP_VM_ENTRY_OID = "1.3.6.1.4.1.60687.1.1.104.1.1";

    String CLOUD_SNMP_VROUTER_OID = "1.3.6.1.4.1.60687.1.1.105";
    String CLOUD_SNMP_VROUTER_ENTRY_OID = "1.3.6.1.4.1.60687.1.1.105.1.1";
}
