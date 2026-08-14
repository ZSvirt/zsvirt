package org.zstack.network.service.virtualrouter.vyos;

import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.utils.path.PathUtil;

/**
 * Created by xing5 on 2016/10/31.
 */
public interface VyosConstants {
    String VYOS_VM_TYPE = "vrouter";
    String VYOS_ROUTER_PROVIDER_TYPE = "vrouter";

    String ANSIBLE_PLAYBOOK_NAME = "zvr.py";
    String ANSIBLE_MODULE_PATH = "ansible/zvr";

    String PRIVATE_L3_FIREWALL_DEFAULT_ACTION = "reject";

    NetworkServiceProviderType PROVIDER_TYPE = new NetworkServiceProviderType(VyosConstants.VYOS_ROUTER_PROVIDER_TYPE);

    enum BootstrapInfoKey {
        vyosPassword
    }

    String VYOS_VERSION_PATH = "ansible/zvr/version";
    int VYOS_VERSION_LENGTH = 4;

    String REPLACE_FIREWALL_WITH_IPTBALES = "SkipVyosIptables";
    String HA_STATUS = "haStatus";
    String CONFIG_ENABLE_VYOS = "EnableVyosCmd";

    /* in old version, vpc snat is disabled in mn node, but it's not delete in vyos node, which is fix in http://jira.zstack.io/browse/ZSTAC-27851
    *  so when upgrade before 3.9.0.0, mn will reconnect virtual router, during reconnection, the snat rules should be deleted*/
    String SNAT_REBUILD_VERSION = "3.9.0.1";

    String VYOS_VMWARE_ALLOW_NIC_HOT_PLUGIN_VERSION = "4.2.0.0";

    int NTP_PORT = 123;
    int DNS_PORT = 53;
}
