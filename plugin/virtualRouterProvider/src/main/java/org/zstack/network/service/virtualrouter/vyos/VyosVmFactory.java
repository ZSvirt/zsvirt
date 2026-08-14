package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmType;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.network.service.lb.LoadBalancerConstants;
import org.zstack.network.service.vip.VipGetUsedPortRangeExtensionPoint;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalConfig;
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalProperty;
import org.zstack.utils.RangeSet;
import org.zstack.utils.Utils;
import org.zstack.utils.VipUseForList;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by weiwang on 19/09/2017
 */
public class VyosVmFactory extends VyosVmBaseFactory implements VipGetUsedPortRangeExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VyosVmFactory.class);
    public static ApplianceVmType applianceVmType = new ApplianceVmType(VyosConstants.VYOS_VM_TYPE);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public ApplianceVmType getApplianceVmType() {
        return applianceVmType;
    }

    @Override
    public RangeSet getVipUsePortRange(String vipUuid, String protocol, VipUseForList useForList) {
        RangeSet portRangeList = new RangeSet();
        List<RangeSet.Range> portRanges = new ArrayList<RangeSet.Range>();
        portRangeList.setRanges(portRanges);

        VipVO vipVO = dbf.findByUuid(vipUuid, VipVO.class);
        /* system vip is the vip of public ip of vpc or vpc ha group */
        boolean hasSnat = vipVO.getServicesTypes().contains(NetworkServiceType.SNAT.toString());
        if (vipVO.isSystem()) {
            if (protocol.equalsIgnoreCase(LoadBalancerConstants.LB_PROTOCOL_UDP) && hasSnat){
                portRanges.add(new RangeSet.Range(VyosConstants.DNS_PORT, VyosConstants.DNS_PORT, true));
                portRanges.add(new RangeSet.Range(VyosConstants.NTP_PORT, VyosConstants.NTP_PORT, true));
            }

            if (protocol.equalsIgnoreCase(LoadBalancerConstants.LB_PROTOCOL_TCP) && hasSnat){
                portRanges.add(new RangeSet.Range(VyosConstants.DNS_PORT, VyosConstants.DNS_PORT, true));

                int sshPort = VirtualRouterGlobalConfig.SSH_PORT.value(Integer.class);
                portRanges.add(new RangeSet.Range(sshPort, sshPort, true));

                int agentPort = VirtualRouterGlobalProperty.AGENT_PORT;
                portRanges.add(new RangeSet.Range(agentPort, agentPort, true));
            }
        }

        return portRangeList;
    }
}
