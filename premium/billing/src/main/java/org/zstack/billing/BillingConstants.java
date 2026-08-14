package org.zstack.billing;

import org.zstack.header.vm.VmInstanceState;

import java.util.Arrays;
import java.util.List;

/**
 * Created by frank on 2/23/2016.
 */
public class BillingConstants {
    public static final String SERVICE_ID = "billing";

    public static final String SPENDING_CPU = "cpu";
    public static final String SPENDING_MEMORY = "memory";
    public static final String SPENDING_ROOT_VOLUME = "rootVolume";
    public static final String SPENDING_DATA_VOLUME = "dataVolume";
    public static final String SPENDING_SNAPSHOT = "snapShot";
    public static final String SPENDING_PCI_DEVICE = "gpu";
    public static final String SPENDING_BAREMETAL2_INSTANCE = "bareMetal2Instance";

    public static final String SPENDING_TYPE_VM = "VM";
    public static final String SPENDING_TYPE_BAREMETAL2_INSTANCE = "bareMetal2Instance";
    public static final String SPENDING_TYPE_DATA_VOLUME = "dataVolume";
    public static final String SPENDING_TYPE_ROOT_VOLUME = "rootVolume";
    public static final String SPENDING_TYPE_SNAPSHOT = "snapShot";
    public static final String SPENDING_TYPE_PCI_DEVICE = "gpu";

    public static final String SPENDING_VM_NIC_BANDWIDTH_IN = "pubIpVmNicBandwidthIn";
    public static final String SPENDING_VM_NIC_BANDWIDTH_OUT = "pubIpVmNicBandwidthOut";
    public static final String SPENDING_PUBLIC_IP_VM_NIC_BANDWIDTH = "pubIpVmNicBandwidth";

    public static final String SPENDING_VIP_BANDWIDTH_IN = "pubIpVipBandwidthIn";
    public static final String SPENDING_VIP_BANDWIDTH_OUT = "pubIpVipBandwidthOut";
    public static final String SPENDING_PUBLIC_IP_VIP_BANDWIDTH = "pubIpVipBandwidth";

    public static final String SPENDING_TYPE_ALL = "all";

    public static final List<String> VM_STATUS_ASSOCIATED_WITH_THE_BILL = Arrays.asList(
            VmInstanceState.Running.toString(), VmInstanceState.Stopped.toString(),
            VmInstanceState.Destroyed.toString(), VmInstanceState.Paused.toString());

    public static final List<String> BAREMETAL2_STATUS_ASSOCIATED_WITH_THE_BILL = Arrays.asList(
            VmInstanceState.Running.toString(), VmInstanceState.Stopped.toString(),
            VmInstanceState.Destroyed.toString(), VmInstanceState.Paused.toString(),
            VmInstanceState.Unknown.toString());

    public static final String GLOBAL_DEFAULT_PRICE_TABLE_UUID = "12a087c058cc45d5bf80a605f17c0083";
}
