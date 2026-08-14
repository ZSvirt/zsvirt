package org.zstack.network.service.virtualrouter.dhcp;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkDnsVO;
import org.zstack.header.network.l3.L3NetworkDnsVO_;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefInventory;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.AddDhcpEntryCmd;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.AddDhcpEntryRsp;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.DhcpInfo;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterSyncDHCPOnStartFlow implements Flow {
	private static final CLogger logger = Utils.getLogger(VirtualRouterSyncDHCPOnStartFlow.class);
	
	@Autowired
	private DatabaseFacade dbf;
	@Autowired
	private VirtualRouterManager vrMgr;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;

	private List<String> getDns(String l3NetworkUuid) {
		SimpleQuery<L3NetworkDnsVO> q = dbf.createQuery(L3NetworkDnsVO.class);
		q.select(L3NetworkDnsVO_.dns);
		q.add(L3NetworkDnsVO_.l3NetworkUuid, Op.EQ, l3NetworkUuid);
		return q.listValue();
	}

    private boolean hasSnatService(L3NetworkInventory l3nw) {
        for (NetworkServiceL3NetworkRefInventory ref : l3nw.getNetworkServices()) {
            if (ref.getNetworkServiceType().equals(NetworkServiceType.SNAT.toString())) {
                return true;
            }
        }

        return false;
    }

	@Transactional(readOnly = true)
	private List<DhcpInfo> getUserVmNicsOnNetwork(VirtualRouterVmInventory vr, String l3NetworkUuid) {
		String sql = "select vm.uuid, vm.defaultL3NetworkUuid, nic.uuid, l3.dnsDomain, nic.l3NetworkUuid from VmNicVO nic, VmInstanceVO vm, L3NetworkVO l3 where l3.uuid = vm.defaultL3NetworkUuid and vm.state = (:vmState) and nic.vmInstanceUuid = vm.uuid and vm.type = :vmType and nic.l3NetworkUuid = :l3uuid";
		TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
		q.setParameter("l3uuid", l3NetworkUuid);
        q.setParameter("vmType", VmInstanceConstant.USER_VM_TYPE);
        q.setParameter("vmState", VmInstanceState.Running);
        List<Tuple> ts = q.getResultList();

        L3NetworkVO l3vo = dbf.getEntityManager().find(L3NetworkVO.class, l3NetworkUuid);
        L3NetworkInventory l3inv = L3NetworkInventory.valueOf(l3vo);

		List<DhcpInfo> infos = new ArrayList<DhcpInfo>(ts.size());
		if (vr.getGuestNicByL3NetworkUuid(l3NetworkUuid) == null) {
		    return infos;
        }

        Map<String, List<VmNicVO>> defaultNicMap = new HashMap<>();
		List<String> defaultNicUuids = new ArrayList<>();
        for (Tuple t : ts) {
            String defaultL3Uuid = t.get(1, String.class);
            String nicUuid = t.get(2, String.class);
            String nicL3Uuid = t.get(4, String.class);
            if (nicL3Uuid.equals(defaultL3Uuid)) {
                defaultNicUuids.add(nicUuid);
            }
        }

        if (!defaultNicUuids.isEmpty()) {
            List<VmNicVO> nics = Q.New(VmNicVO.class).in(VmNicVO_.uuid, defaultNicUuids).list();
            for (VmNicVO nic : nics) {
                List<VmNicVO> value = defaultNicMap.get(nic.getVmInstanceUuid());
                if (value == null) {
                    value = new ArrayList<>();
                }
                value.add(nic);
                defaultNicMap.put(nic.getVmInstanceUuid(), value);
            }
        }

		for (Tuple t : ts) {
			String vmUuid = t.get(0, String.class);
            String defaultL3Uuid = t.get(1, String.class);
			String nicUuid = t.get(2, String.class);
            String defaultL3DnsDomain = t.get(3, String.class);
			
			VmNicVO nic = dbf.getEntityManager().find(VmNicVO.class, nicUuid);
			DhcpInfo info  = new DhcpInfo();
			info.setGateway(nic.getGateway());
			info.setIp(nic.getIp());
			info.setMac(nic.getMac());
            info.setVrNicMac(vr.getGuestNicByL3NetworkUuid(l3NetworkUuid).getMac());
			info.setNetmask(nic.getNetmask());
            if (l3NetworkUuid.equals(defaultL3Uuid)) {
                if (defaultNicMap.get(nic.getVmInstanceUuid()) != null && defaultNicMap.get(nic.getVmInstanceUuid()).size() > 1) {
                    info.setDefaultL3Network(nic.equals(VmNicVO.findTheEarliestOne(defaultNicMap.get(nic.getVmInstanceUuid()))));
                }
                else {
                    info.setDefaultL3Network(true);
                }
            }

            if (info.isDefaultL3Network()) {
                info.setDnsDomain(defaultL3DnsDomain);
                String hostname = VmSystemTags.HOSTNAME.getTokenByResourceUuid(vmUuid, VmSystemTags.HOSTNAME_TOKEN);
                if (hostname != null) {
                    if (info.getDnsDomain() != null) {
                        hostname = String.format("%s.%s", hostname, info.getDnsDomain());
                    }
                    info.setHostname(hostname);
                }
                info.setDns(Arrays.asList(vr.getGuestNicByL3NetworkUuid(l3NetworkUuid).getIp()));
            }

            if (hasSnatService(l3inv)) {
                info.setDns(Arrays.asList(vr.getGuestNicByL3NetworkUuid(l3NetworkUuid).getIp()));
            } else {
                info.setDns(getDns(l3NetworkUuid));
            }

			infos.add(info);
		}
		return infos;
	}
	
    @Override
    public void run(final FlowTrigger chain, Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());

        List<String> nwServed = vr.getGuestL3Networks();
        List<String> l3Uuids = vrMgr.selectL3NetworksNeedingSpecificNetworkService(nwServed, NetworkServiceType.DHCP);
        if (l3Uuids.isEmpty()) {
            chain.next();
            return;
        }

        if (VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(vr.getUuid()) && !VirtualRouterSystemTags.VR_DHCP_ROLE.hasTag(vr.getUuid())) {
            chain.next();
            return;
        }

        new VirtualRouterRoleManager().makeDhcpRole(vr.getUuid());

        AddDhcpEntryCmd cmd = new AddDhcpEntryCmd();
        cmd.setRebuild(true);
        for (String l3uuid : l3Uuids) {
            List<DhcpInfo> infos = getUserVmNicsOnNetwork(vr, l3uuid);
            cmd.getDhcpEntries().addAll(infos);
        }

        if (cmd.getDhcpEntries().isEmpty()) {
            chain.next();
            return;
        }

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setPath(VirtualRouterConstant.VR_ADD_DHCP_PATH);
        msg.setVmInstanceUuid(vr.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("unable to program dhcp entries served by virtual router[uuid:%s, ip:%s], %s", vr.getUuid(), vr.getManagementNic().getIp(), reply.getError()));
                    chain.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                AddDhcpEntryRsp ret =  re.toResponse(AddDhcpEntryRsp.class);
                if (!ret.isSuccess()) {
                    ErrorCode err = operr("unable to program dhcp entries served by virtual router[uuid:%s, ip:%s], %s", vr.getUuid(), vr.getManagementNic().getIp(), ret.getError());
                    chain.fail(err);
                } else {
                    logger.debug(String.format("successfully programmed dhcp entries served by virtual router[uuid:%s, ip:%s]", vr.getUuid(), vr.getManagementNic().getIp()));
                    chain.next();
                }
            }
        });
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        chain.rollback();
    }
}
