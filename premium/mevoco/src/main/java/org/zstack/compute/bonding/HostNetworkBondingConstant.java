package org.zstack.compute.bonding;

public interface HostNetworkBondingConstant {
    public static final String SERVICE_ID = "bonding";
    public static final String ACTION_CATEGORY = "bonding";

    public static final String OVS_BONDING_TYPE = "OvsBonding";
    public static final String LINUX_BONDING_TYPE = "LinuxBonding";

    public static final String BONDING_MODE_LACP = "802.3ad";
    public static final String BONDING_MODE_AB = "active-backup";

    public static final String BONDING_XMIT_HASH_POLICY_LAYER_TWO = "layer2";
    public static final String BONDING_XMIT_HASH_POLICY_LAYER_TWO_AND_THREE = "layer2+3";
    public static final String BONDING_XMIT_HASH_POLICY_LAYER_THREE_AND_FOUR = "layer3+4";
    public static final String BONDING_XMIT_HASH_POLICY_NULL = "null";

    public static final String MII_STATUS_UP = "up";
    public static final String MII_STATUS_DOWN = "down";

    public static final int BONDING_NAME_MAX = 10;
    public static final String BONDING_NAME_REGEX = "^(?![0-9])[A-Za-z0-9_-]+$";


    public static enum Param {
        BONDINGINV,
        PREBONDINGVO
    }
}
