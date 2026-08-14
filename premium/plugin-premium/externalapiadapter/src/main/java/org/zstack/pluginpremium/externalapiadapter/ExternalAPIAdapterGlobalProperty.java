package org.zstack.pluginpremium.externalapiadapter;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*;

@GlobalPropertyDefinition
public class ExternalAPIAdapterGlobalProperty {
    @GlobalProperty(name = "ExternalAPIAdapter.ZSClient.hostname", defaultValue = "127.0.0.1")
    public static String ZSCLIENT_HOSTNAME;

    @GlobalProperty(name = "ExternalAPIAdapter.ZSClient.port", defaultValue = "8080")
    @Deprecated
    public static int ZSCLIENT_PORT;

    @GlobalProperty(name = "ExternalAPIAdapter.ZSClient.contextPath", defaultValue = "zstack")
    public static String ZSCLIENT_CONTEXTPATH;

    // format: AccountName::Password::AccessKey::AccessSecret
    @GlobalProperty(name = "ExternalAPIAdapter.ZStackAccountAccess")
    public static String ZSTACK_ACCOUNT_PASSWORD_ACCESSKEY_ACCESSSECRET;

    @GlobalProperty(name = "ExternalAPIAdapter.ECS.Endpoint.url")
    public static String ECS_ENDPOINT_URL;

    @GlobalProperty(name = "ExternalAPIAdapter.ECS.Endpoint.regionId")
    public static String ECS_ENDPOINT_REGIONID;

    @GlobalProperty(name = "ExternalAPIAdapter.publicL3NetworkUuid")
    public static String PUBLICL3NETWORKUUID;

    @GlobalProperty(name = "ExternalAPIAdapter.privateL3NetworkUuid")
    public static String PRIVATEL3NETWORKUUID;

    @GlobalProperty(name = "ExternalAPIAdapter.backupStorageUuid")
    public static String BACKUPSTORAGE_UUID;

    @GlobalProperty(name = "ExternalAPIAdapter.primaryStorageUuid")
    public static String PRIMARYSTOARGE_UUID;

    @GlobalProperty(name = "ExternalAPIAdapter.virtualRouterOfferingUuid")
    public static String VIRTUALROUTEROFFERINGUUID;

    @GlobalProperty(name = "ExternalAPIAdapter.vxlanPoolUuid")
    public static String VXLAN_POOL_UUID;

    @GlobalProperty(name = "NetworkService.securityGroupUuid")
    public static String SECURITYGROUP_SERVICE_UUID;

    @GlobalProperty(name = "NetworkService.vrouterUuid")
    public static String VROUTER_SERVICE_UUID;

    @GlobalProperty(name = "NetworkService.flat")
    public static String FLAT_SERVICE_UUID;

    @GlobalProperty(name = "NetworkService.virtualRouterUuid")
    public static String VIRTUALROUTER_SERVICE_UUID;

    @GlobalProperty(name = "NetworkService.aliDNS", defaultValue = "223.5.5.5")
    public static String ALI_SERVICE_DNS;

    @GlobalProperty(name = "ExternalAPIAdapter.ummAccessKeyId", defaultValue = "ak_id")
    public static String UMM_ACCESSKEY_ID;

    @GlobalProperty(name = "ExternalAPIAdapter.ummAccessKeySecret", defaultValue = "ak_secret")
    public static String UMM_ACCESSKEY_SECRET;

    @GlobalProperty(name = "ExternalAPIAdapter.ummBaseUrl", defaultValue = "ak_url")
    public static String UMM_BASE_URL;

    @GlobalProperty(name = "SlbQuota.slbs-per-user", defaultValue = SLB_QUOTA_CONST.SLB_PER_SERVER_DEF)
    public static String SLB_PER_USER;

    @GlobalProperty(name = "SlbQuota.server-cers-per-region", defaultValue = SLB_QUOTA_CONST.SERVER_CA_PER_REGION_DEF)
    public static String SERVER_CA_PER_REGION;

    @GlobalProperty(name = "SlbQuota.client-ca-cers-per-region", defaultValue = SLB_QUOTA_CONST.CLIENT_CA_PER_REGION_DEF)
    public static String CLIENT_CA_PER_REGION;

    @GlobalProperty(name = "SlbQuota.slbs-per-backendserver", defaultValue = SLB_QUOTA_CONST.SLB_PER_SERVER_DEF)
    public static String SLB_PER_SERVER;

    @GlobalProperty(name = "SlbQuota.backendservers-per-slb", defaultValue = SLB_QUOTA_CONST.SERVER_PER_SLB_DEF)
    public static String SERVER_PER_SLB;

    @GlobalProperty(name = "SlbQuota.listeners-per-slb", defaultValue = SLB_QUOTA_CONST.LISTENER_PER_SLB_DEF)
    public static String LISTENER_PER_SLB;

    @GlobalProperty(name = "SlbQuota.rules-per-listener", defaultValue = SLB_QUOTA_CONST.RULE_PER_LISTENER_DEF)
    public static String RULE_PER_LISTENER;

    @GlobalProperty(name = "SlbQuota.domain-extensions-per-listener", defaultValue = SLB_QUOTA_CONST.DOMAIN_EXT_PER_LISTENER_DEF)
    public static String DOMAIN_EXT_PER_LISTENER;

    @GlobalProperty(name = "SlbQuota.acls-per-region", defaultValue = SLB_QUOTA_CONST.ACL_PER_REGION_DEF)
    public static String ACL_PER_REGION;

    @GlobalProperty(name = "SlbQuota.listeners-per-acl", defaultValue = SLB_QUOTA_CONST.LISTENER_PER_ACL_DEF)
    public static String LISTENER_PER_ACL;

    @GlobalProperty(name = "SlbQuota.entries-per-acl", defaultValue = SLB_QUOTA_CONST.ENTRY_PER_ACL_DEF)
    public static String ENTRY_PER_ACL;

}
