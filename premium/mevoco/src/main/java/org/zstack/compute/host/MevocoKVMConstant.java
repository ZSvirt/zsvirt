package org.zstack.compute.host;

import org.zstack.header.configuration.PythonClass;

/**
 * Created by mingjian.deng on 16/12/1.
 */
@PythonClass
public interface MevocoKVMConstant {
    public static final String KVM_VM_CHANGE_PASSWORD_PATH = "/vm/changepasswd";
    public static final String KVM_VM_CHECK_VOLUME_PATH = "/vm/volume/check";
    public static final String KVM_VM_RECOVER_VOLUMES_PATH = "/vm/recover/volumes";
    public static final String KVM_VM_CHECK_RECOVER_PATH = "/vm/recover/check";
    public static final String SET_VOLUME_BANDWIDTH = "/set/volume/bandwidth";
    public static final String DELETE_VOLUME_BANDWIDTH = "/delete/volume/bandwidth";
    public static final String GET_VOLUME_BANDWIDTH = "/get/volume/bandwidth";
    public static final String SET_NIC_QOS = "/set/nic/qos";
    public static final String GET_NIC_QOS = "/get/nic/qos";
    public static final String CHECK_MOUNT_DOMAIN = "/check/mount/domain";
    public static final String RESIZE_VOLUME = "/volume/resize";
    public static final String TAKE_VOLUMES_SNAPSHOT = "/vm/volumes/takesnapshot";
    public static final String BLOCK_STREAM = "/vm/volume/blockstream";
    public static final String ENABLE_HUGEPAGE = "/host/enable/hugepage";
    public static final String DISABLE_HUGEPAGE = "/host/disable/hugepage";
    public static final String IDENTIFY_HOST = "/host/identify";
    public static final String LOCATE_HOST_NETWORK_INTERFACE = "/host/locate/networkinterface";
    public static final String SET_IP_ON_HOST_NETWORK_INTERFACE = "/host/setip/networkinterface";
    public static final String SET_SERVICE_TYPE_ON_HOST_NETWORK_INTERFACE = "/host/setservicetype/networkinterface";
    public static final String CHECK_INTERFACE_VLAN = "/host/checkvlan/networkinterface";
    public static final String GET_INTERFACE_VLAN = "/host/getvlan/networkinterface";
    public static final String GET_INTERFACE_NAME = "/host/getname/networkinterface";
    public static final String CHANGE_PASSWORD = "/host/changepassword";
    public static final String GET_HOST_NETWORK_FACTS = "/host/networkfacts";
    public static final String GET_HOST_BONDING_FACTS = "/host/networkfacts/bonding";
    public static final String GET_CLUSTER_HOST_NETWORK_FACTS = "/cluster/hostsnetworkfacts";
    public static final String GET_HOST_PHYSICAL_CPU_FACTS = "/host/physicalcpufacts";
    public static final String GET_HOST_PHYSICAL_MEMORY_FACTS = "/host/physicalmemoryfacts";
    public static final String UPDATE_HOST_OVS_CPU_PINNING = "/host/ovs/cpu-pin/update";
    public static final String SET_BRIDGE_ROUTER_PORT = "/host/bridge/routerport";
    public static final String ENABLE_ZERO_COPY = "/host/enable/zerocopy";
    public static final String DISABLE_ZERO_COPY = "/host/disable/zerocopy";
    public static final String ADD_BRIDGE_FDB_ENTRY_PATH = "/bridgefdb/add";
    public static final String SET_VM_EMULATOR_PINNING = "/vm/emulatorpinning";
    public static final String DEL_BRIDGE_FDB_ENTRY_PATH = "/bridgefdb/delete";
    public static final String SET_HOST_PHYSICAL_NIC_MONITOR = "/host/physicalNic/update";
    public static final String SYNC_VM_CLOCK_PATH = "/vm/clock/sync";
    public static final String SET_SYNC_VM_CLOCK_TASK_PATH = "/vm/clock/sync/task";
    public static final String INIT_ZWATCH_METRIC_MONITOR = "/host/zwatchMetricMonitor/init";
    public static final String KVM_VM_GUESTTOOLS_STATE_PATH = "/vm/guesttools/state";
    public static final String KVM_VM_CONFIG_SYNC_PORTS_PATH = "/vm/configsync/ports";
    public static final String KVM_VM_CONFIG_SYNC_SPECIFICATION_PATH = "/vm/configsync/specification";
    public static final String SET_VM_HOSTNAME_PATH = "/vm/set/hostname";
    public static final String SET_VM_DNS_PATH = "/vm/set/dns";
    public static final String APPLY_MEMORY_BALLOON_PATH = "/vm/apply/memory/balloon";
    public static final String SET_VM_IOTHREAD_PIN_PATH = "/vm/setiothreadpin";
    public static final String GET_VM_IOTHREAD_PIN_PATH = "/vm/getiothreadpin";
    public static final String DEL_VM_IOTHREAD_PIN_PATH = "/vm/deliothreadpin";
    public static final String SET_VM_SCSI_CONTROLLER_PATH = "/vm/setscsicontroller";
    public static final String DEL_VM_SCSI_CONTROLLER_PATH = "/vm/delscsicontroller";
    public static final String BATCH_UPDATE_BRIDGE_PATH = "/network/bridge/batchupdate";
    public static final String SET_VM_VF_NIC_STATE = "/vm/vfnic/state";
    public static final String ADD_BONDING_PATH = "/network/bonding/create";
    public static final String UPDATE_BONDING_PATH = "/network/bonding/update";
    public static final String DEL_BONDING_PATH = "/network/bonding/delete";
    public static final String ATTACH_NIC_TO_BONDING_PATH = "/network/bonding/attachnic";
    public static final String DETACH_NIC_FROM_BONDING_PATH = "/network/bonding/detachnic";
    public static final String PRECONFIGURE_OVSDPDK_PATH = "/network/ovsdpdk/resource/configure";
    public static final String KVM_SYNC_VDPA_PATH = "/network/ovsdpdk/sync";
    public static final String GET_KERNEL_INTERFACE_PATH = "/host/kernelinterface/get";
    public static final String SET_KERNEL_INTERFACE_PATH = "/host/kernelinterface/set";
    public static final String KVM_UPDATE_HOST_ISCSI_INITIATOR_NAME_PATH = "/host/iscsiinitiatorname/update";
    String SET_HOST_PHYSICAL_MEMORY_MONITOR = "/host/physical/memory/monitor/start";
}
