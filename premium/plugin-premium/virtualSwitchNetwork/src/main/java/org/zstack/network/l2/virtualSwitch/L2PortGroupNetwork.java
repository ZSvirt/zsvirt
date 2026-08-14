package org.zstack.network.l2.virtualSwitch;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostParam;
import org.zstack.header.network.l2.APIUpdateL2NetworkVirtualNetworkIdMsg;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.network.l2.L2NoVlanNetwork;
import org.zstack.network.l2.virtualSwitch.header.L2PortGroupNetworkInventory;
import org.zstack.network.l2.virtualSwitch.header.L2PortGroupNetworkVO;
import org.zstack.network.l2.virtualSwitch.header.PortGroupVO;
import org.zstack.network.l2.virtualSwitch.header.PortGroupVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by shixin.ruan on 2023/09/01.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class L2PortGroupNetwork extends L2NoVlanNetwork {
    private static final CLogger logger = Utils.getLogger(L2PortGroupNetwork.class);

    public L2PortGroupNetwork(L2NetworkVO self) {
        super(self);
    }

    public L2PortGroupNetwork() {
    }

    private L2PortGroupNetworkVO getSelf() {
        return (L2PortGroupNetworkVO) self;
    }

    @Override
    protected L2NetworkInventory getSelfInventory() {
        return L2PortGroupNetworkInventory.valueOf(getSelf());
    }

    @Override
    protected List<HostInventory> getAttachableHostsInCluster(String clusterUuid, List<HostParam> hostParams) {
        List<HostInventory> oldHosts = super.getAttachableHostsInCluster(clusterUuid, hostParams);
        if (oldHosts.isEmpty()) {
            return oldHosts;
        }

        L2PortGroupNetworkInventory pgInv = (L2PortGroupNetworkInventory) getSelfInventory();
        List<HostInventory> ret = new ArrayList<>();
        for (HostInventory host : oldHosts) {
            if (VirtualSwitchUtils.isUplinkGroupExist(pgInv.getvSwitchUuid(), host.getUuid())) {
                ret.add(host);
            }
        }

        return ret;
    }

    @Override
    protected String makeBridgeName() {
        return VirtualSwitchUtils.makeBridgeName(getSelf().getvSwitchUuid(), getSelf().getUuid(), getSelf().getVlanId());
    }

    @Override
    protected void checkNetworkPhysicalInterface(final List<HostInventory> hosts, final Completion completion) {
        completion.success();
    }

    @Override
    protected Flow updateL2NetworkVirtualNetworkIdInDb(final APIUpdateL2NetworkVirtualNetworkIdMsg msg) {
        return new Flow() {
            String __name__ = "update-port-group-vlan-id-in-db";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (!self.getVirtualNetworkId().equals(msg.getVirtualNetworkId())) {
                    L2PortGroupNetworkVO vo = getSelf();
                    data.put(L2NetworkConstant.Param.OLD_VIRTUAL_NETWORK_ID.toString(), vo.getVirtualNetworkId());
                    vo.setVlanId(msg.getVirtualNetworkId());
                    vo.setVirtualNetworkId(vo.getVlanId());
                    dbf.updateAndRefresh(vo);

                    List<PortGroupVO> pgs = Q.New(PortGroupVO.class).eq(PortGroupVO_.l2NetworkUuid, vo.getUuid()).list();
                    for (PortGroupVO pg : pgs) {
                        pg.setVlanId(msg.getVirtualNetworkId());
                        dbf.updateAndRefresh(pg);
                    }
                }
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                Integer oldVlan = (Integer) data.get(L2NetworkConstant.Param.OLD_VIRTUAL_NETWORK_ID.toString());
                if (oldVlan == null) {
                    trigger.rollback();
                    return;
                }

                L2PortGroupNetworkVO vo = getSelf();
                vo.setVlanId(oldVlan);
                vo.setVirtualNetworkId(vo.getVlanId());
                dbf.updateAndRefresh(vo);

                List<PortGroupVO> pgs = Q.New(PortGroupVO.class).eq(PortGroupVO_.l2NetworkUuid, vo.getUuid()).list();
                for (PortGroupVO pg : pgs) {
                    pg.setVlanId(oldVlan);
                    dbf.updateAndRefresh(pg);
                }
                trigger.rollback();
            }
        };
    }
}