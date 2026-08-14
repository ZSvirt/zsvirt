package org.zstack.network.service.vipQos.flat;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shixin on 2017/12/15.
 */
public class FlatVipQosSimulatorConfig {
    public volatile boolean VipQosSuccess = true;
    public List<FlatVipQosBackend.SetVipQosCmd> SetVipQosCmdList = new ArrayList<>();
    public List<FlatVipQosBackend.DeleteVipQosCmd> DeleteVipQosCmdList = new ArrayList<>();
    public List<FlatVipQosBackend.DeleteVipAllQosCmd> DeleteVipAllQosCmdList = new ArrayList<>();
}
