package org.zstack.network.hostNetworkInterface.lldp;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public interface LldpConstant {
    String SERVICE_ID = "hostNetwork.lldp";
    String ACTION_CATEGORY = "hostNetwork.lldp";

    enum mode {
        rx_only,
        tx_only,
        rx_and_tx,
        disable
    }

    String CHANGE_LLDP_MODE_PATH = "/network/lldp/changemode";
    String GET_LLDP_INFO_PATH = "/network/lldp/get";
    String APPLY_LLDP_CONFIG_PATH = "/network/lldp/apply";
}
