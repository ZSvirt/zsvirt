package org.zstack.kvm;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

import java.util.List;

/**
 */
@GlobalPropertyDefinition
public class KVMGlobalProperty {
    @GlobalProperty(name="KvmAgent.agentPackageName", defaultValue = "kvmagent-5.0.0.tar.gz")
    public static String AGENT_PACKAGE_NAME;
    @GlobalProperty(name="KvmAgent.agentUrlRootPath", defaultValue = "")
    public static String AGENT_URL_ROOT_PATH;
    @GlobalProperty(name="KvmAgent.agentUrlScheme", defaultValue = "http")
    public static String AGENT_URL_SCHEME;
    @GlobalProperty(name="KvmAgent.port", defaultValue = "7070")
    public static int AGENT_PORT;
    @GlobalProperty(name="KvmAgentServer.port", defaultValue = "10001")
    public static int AGENT_SERVER_PORT;
    @GlobalProperty(name="KvmHost.iptables.rule.", defaultValue = "")
    public static List<String> IPTABLES_RULES;
    @GlobalProperty(name="KvmHost.maxThreads.ratio", defaultValue = "0.6")
    public static double KVM_HOST_MAX_THREDS_RATIO;
    @GlobalProperty(name="KvmAgent.tcpServerPort", defaultValue = "7123")
    public static int TCP_SERVER_PORT;
    @GlobalProperty(name="KvmHost.takeOverFlagPath", defaultValue = "/var/run/zstack/takeOver")
    public static String TAKEVOERFLAGPATH;
    @GlobalProperty(name="MN.network.", defaultValue = "")
    public static List<String> MN_NETWORKS;
    @GlobalProperty(name = "host.skip.packages", defaultValue = "qemu, qemu-kvm, qemu-kvm-ev, qemu-img, qemu-img-ev")
    public static String SKIP_PACKAGES;
    @GlobalProperty(name = "host.stop.shutdown.vm", defaultValue = "false")
    public static boolean HOST_STOP_SHUTDOWN_VM;

}
