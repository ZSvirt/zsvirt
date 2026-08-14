package org.zstack.appliancevm;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApplianceVmConstant {
    public static final String APPLIANCE_VM_TYPE = "ApplianceVm";
    public static final String SERVICE_ID = "applianceVm";

    public static final String KVM_CHANNEL_AGENT_PATH = "/var/lib/zstack/kvm/agentSocket";

    public static final String ANSIBLE_PLAYBOOK_NAME = "appliancevm.py";

    public static final String ANSIBLE_MODULE_PATH = "ansible/appliancevm";

    public static final String APPLIANCE_VM_ABNORMAL_FILE_REPORT = "/appliancevm/abnormalfiles/report";
    public static final String ABNORMAL_FILE_MAX_SIZE = "abnormalFileMaxSize";

    public enum BootstrapParams {
        managementNic,
        additionalNics,
        publicKey,
        sshPort,
        uuid,
        managementNodeIp,
        managementNodeCidr,
        additionalL3Uuids,
    }

    public static enum Params {
        applianceVmSubType,
        applianceVmSpec,
        applianceVmInfoForPostLifeCycle,
        applianceVmFirewallRules,
        isReconnect,
        managementNicIp,
        applianceVmUuid,
        timeout,
        rebuildSnat,
    }

    public static final String REFRESH_FIREWALL_PATH = "/appliancevm/refreshfirewall";
    public static final String ECHO_PATH = "/appliancevm/echo";
    public static final String INIT_PATH = "/appliancevm/init";
}
