package org.zstack.network.service.vipQos.vyos;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shixin on 2017/12/15.
 */
public class VyosVipQosSimulatorConfig {
    public volatile boolean VipQosSuccess = true;
    public List<VyosVipQosBackend.SetVipQosCmd> SetVipQosCmdList = new ArrayList<>();
    public List<VyosVipQosBackend.DeleteVipQosCmd> DeleteVipQosCmdList = new ArrayList<>();
    public List<VyosVipQosBackend.DeleteVipAllQosCmd> DeleteVipAllQosCmdList = new ArrayList<>();
}
