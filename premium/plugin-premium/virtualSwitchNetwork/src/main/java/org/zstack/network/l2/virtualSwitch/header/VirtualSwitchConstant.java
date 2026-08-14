package org.zstack.network.l2.virtualSwitch.header;

public class VirtualSwitchConstant {

    public static final String VIRTUAL_SWITCH_NETWORK_TYPE = "virtualSwitch";
    public static final String VIRTUAL_SWITCH_SERVICE_ID = "virtualSwitch";

    public static final String PORT_GROUP_NETWORK_TYPE = "portGroup";
    public static final String PORT_GROUP_SERVICE_ID = "portGroup";

    public static final String HOST_KERNEL_INTERFACE_TYPE = "hostKernelInterface";

    public static final String ACTION_CATEGORY = "virtualSwitch";

    public static final String DEFAULT_VIRTUAL_SWITCH_NAME_FORMAT = "DSwitch %s";
    public static final String DEFAULT_PORT_GROUP_NAME_PREFIX = "DPortGroup";
    public static final String DEFAULT_BRIDGE_NAME_PREFIX = "dvs";
    public static final int MAX_VIRTUAL_SWITCH_INDEX = 9999;

    public static final String PORT_GROUP_SYNC_ID_FORMAT = "operate-port-group-on-vSwitch-%s-vlan-%s";
}
