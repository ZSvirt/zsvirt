package org.zstack.routeProtocol;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmStatus;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.*;
import org.zstack.header.protocol.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vpc.VpcConstants;
import org.zstack.header.vpc.VpcRouterVmVO;
import org.zstack.network.l3.IpRangeHelper;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.operr;

public class RouteProtocolOspfBackend implements VirtualRouterBeforeDetachNicExtensionPoint, VirtualRouterHaGetCallbackExtensionPoint {
    private final static CLogger logger = Utils.getLogger(RouteProtocolManagerImpl.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected VirtualRouterHaBackend haBackend;
    @Autowired
    protected RouterProtocolConfigProxy proxy;
    @Autowired
    private CloudBus bus;
    @Autowired
    private EventFacade evtf;

    public static final String ROUTER_PROTOCOL_REFRESH_OSPF = "/routerprotocol/ospf/refresh";
    public static final String ROUTER_PROTOCOL_GET_OSPF_NEIGHBOR = "/routerprotocol/ospf/neighbor";

    private final String APPLY_OSPF_TASK = "applyOspf";

    public static class NetworkInfo {
        private String nicMac;
        private String network;
        private String areaId;

        public String getNicMac() {
            return nicMac;
        }

        public void setNicMac(String nicMac) {
            this.nicMac = nicMac;
        }

        public String getNetwork() {
            return network;
        }

        public void setNetwork(String network) {
            this.network = network;
        }

        public String getAreaId() {
            return areaId;
        }

        public void setAreaId(String areaId) {
            this.areaId = areaId;
        }
    }

    public static class AreaInfo {
        private String areaId;
        private RouterAreaType type;
        private RouterAreaAuthType authType;
        private String authParam;

        public AreaInfo(RouterAreaVO vo) {
            this.areaId = vo.getAreaId();
            this.authType = vo.getAuthentication();
            this.type = vo.getType();
            if (RouterAreaAuthType.Plaintext.equals(vo.getAuthentication())) {
                this.authParam = vo.getPassword();
            } else if (RouterAreaAuthType.MD5.equals(vo.getAuthentication())) {
                this.authParam = String.format("%d/%s",vo.getKeyId(),vo.getPassword());
            }

        }

        @Override
        public int hashCode() {
            return getAreaId().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof  AreaInfo) {
                return getAreaId().equals(((AreaInfo) obj).getAreaId()) ;
            } else {
                return false;
            }
        }

        public String getAreaId() {
            return areaId;
        }

        public void setAreaId(String areaId) {
            this.areaId = areaId;
        }

        public RouterAreaType getType() {
            return type;
        }

        public void setType(RouterAreaType type) {
            this.type = type;
        }

        public RouterAreaAuthType getAuthType() {
            return authType;
        }

        public void setAuthType(RouterAreaAuthType authType) {
            this.authType = authType;
        }

        public String getAuthParam() {
            return authParam;
        }

        public void setAuthParam(String authParam) {
            this.authParam = authParam;
        }
    }
    public static class SetOspfCmd extends VirtualRouterCommands.AgentCommand {
        private String routerId;
        private List<NetworkInfo> networkInfos;
        private List<AreaInfo> areaInfos;

        public String getRouterId() {
            return routerId;
        }

        public void setRouterId(String routerId) {
            this.routerId = routerId;
        }

        public List<NetworkInfo> getNetworkInfos() {
            return networkInfos;
        }

        public void setNetworkInfos(List<NetworkInfo> networkInfos) {
            this.networkInfos = networkInfos;
        }

        public List<AreaInfo> getAreaInfos() {
            return areaInfos;
        }

        public void setAreaInfos(List<AreaInfo> areaInfos) {
            this.areaInfos = areaInfos;
        }
    }

    public static class SetOspfRsp extends VirtualRouterCommands.AgentResponse {}

    public static class GetOspfNeighborCmd extends VirtualRouterCommands.AgentCommand {
    }

    public static class GetOspfNeighborRsp extends VirtualRouterCommands.AgentResponse {
        public List<Neighbor> neighbors;
    }

    private List<AreaInfo> getArea(String vRouterUuid) {
        String vrUuid = vRouterUuid;
        VpcRouterVmVO vpcVo = dbf.findByUuid(vRouterUuid, VpcRouterVmVO.class);
        if (vpcVo.isHaEnabled()) {
            vrUuid = proxy.getHaUuidOfVpcRouter(vRouterUuid);
        }
        List<AreaInfo> infos = new ArrayList<>();
        List<RouterAreaVO> areas = SQL.New("select area from RouterAreaVO area," +
                "NetworkRouterAreaRefVO ref where area.uuid = ref.routerAreaUuid " +
                "and ref.vRouterUuid = (:routerUuid)").param("routerUuid", vrUuid).list();

        for (RouterAreaVO vo: areas) {
            AreaInfo info = new AreaInfo(vo);
            if (!infos.contains(info)) {
                infos.add(info);
            }
        }
        return infos;
    }

    private List<NetworkInfo> getOspfNetwork(String vRouterUuid) {
        List<NetworkInfo> networks = new ArrayList<>();
        final VirtualRouterVmVO vo = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, vRouterUuid).find();
        final VirtualRouterVmInventory vrinv = VirtualRouterVmInventory.valueOf(vo);

        String vrUuid = vRouterUuid;
        if (vrinv.isHaEnabled()) {
            vrUuid = proxy.getHaUuidOfVpcRouter(vRouterUuid);
        }
        List<NetworkRouterAreaRefVO> refs = Q.New(NetworkRouterAreaRefVO.class)
                                             .eq(NetworkRouterAreaRefVO_.vRouterUuid, vrUuid).list();
        //refs = refs.stream().distinct().collect(Collectors.toList());

        for (NetworkRouterAreaRefVO ref: refs) {
            NetworkInfo network = new NetworkInfo();
            network.areaId = Q.New(RouterAreaVO.class)
                              .select(RouterAreaVO_.areaId)
                              .eq(RouterAreaVO_.uuid, ref.getRouterAreaUuid()).findValue();

            VmNicInventory nic = vrinv.getGuestNicByL3NetworkUuid(ref.getL3NetworkUuid());
            if (nic != null) {
                network.nicMac = nic.getMac();
            } else {
                continue;
            }

            final L3NetworkInventory l3 = L3NetworkInventory.valueOf(dbf.findByUuid(ref.getL3NetworkUuid(), L3NetworkVO.class));
            List<IpRangeInventory> iprs = IpRangeHelper.getNormalIpRanges(l3, IPv6Constants.IPv4);
            if (iprs.isEmpty()) {
                continue;
            }
            network.network = iprs.get(0).getNetworkCidr();
            if (!networks.contains(network)) {
                networks.add(network);
            }
        }
        return networks;
    }

    private String GetRouterId(String virtualRouterUuid) {
        return proxy.getOspfRouterId(virtualRouterUuid);
    }

    void applyOspfToAgent(String virtualRouterUuid, Completion completion) {
        applyOspfToAgent(virtualRouterUuid, true, completion);
    }

    void applyOspfToAgent(String virtualRouterUuid,  Boolean applyEmpty, Completion completion) {
        logger.debug(String.format("Start apply ospf on vRouter: %s", virtualRouterUuid));

        SetOspfCmd cmd = new SetOspfCmd();
        cmd.setAreaInfos(getArea(virtualRouterUuid));
        cmd.setNetworkInfos(getOspfNetwork(virtualRouterUuid));
        cmd.setRouterId(GetRouterId(virtualRouterUuid));

        if (cmd.getAreaInfos().isEmpty() && cmd.getNetworkInfos().isEmpty() && !applyEmpty) {
            completion.success();
            return;
        }

        VirtualRouterAsyncHttpCallMsg vrMsg = new VirtualRouterAsyncHttpCallMsg();
        vrMsg.setVmInstanceUuid(virtualRouterUuid);
        vrMsg.setPath(ROUTER_PROTOCOL_REFRESH_OSPF);
        vrMsg.setCommand(cmd);
        vrMsg.setCheckStatus(false);
        bus.makeTargetServiceIdByResourceUuid(vrMsg, VmInstanceConstant.SERVICE_ID, virtualRouterUuid);
        bus.send(vrMsg, new CloudBusCallBack(completion) {
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    VirtualRouterAsyncHttpCallReply ar = reply.castReply();
                    SetOspfRsp rsp = ar.toResponse(SetOspfRsp.class);
                    if (rsp.isSuccess()) {
                        completion.success();
                        FirewallCanonicalEvents.FirewallRuleChangedData data = new FirewallCanonicalEvents.FirewallRuleChangedData();
                        data.setVirtualRouterUuid(virtualRouterUuid);
                        evtf.fire(FirewallCanonicalEvents.FIREWALL_RULE_CHANGED_PATH, data);
                    } else {
                        completion.fail(operr("operation error, because:%s", rsp.getError()));
                    }
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    private void submitapplyOspfToHaRouter(String vrUuid, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(APPLY_OSPF_TASK);
        task.setOriginRouterUuid(vrUuid);
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    /*
   always apply the ospf to agent under any case, if aggressive is true
    */
    void applyOspf(String virtualRouterUuid, boolean aggressive, Completion completion) {
        List<NetworkInfo> networkInfos = getOspfNetwork(virtualRouterUuid);
        if (networkInfos.isEmpty() && !aggressive) {
            completion.success();
            return;
        }

        /*reconnect router case, the status is Connecting*/
        final VirtualRouterVmVO vo = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, virtualRouterUuid).find();
        if (!VmInstanceState.Running.equals(vo.getState()) ||
                !ApplianceVmStatus.Connected.equals( vo.getStatus())) {
            completion.success();
            return;
        }

        applyOspfToAgent(virtualRouterUuid, new Completion(completion) {
            @Override
            public void success() {
                submitapplyOspfToHaRouter(virtualRouterUuid, new Completion(completion) {
                    @Override
                    public void success() {
                        completion.success();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    /*
    only apply the config when the ospf is configured completely.
     */
    void applyOspf(String virtualRouterUuid, Completion completion) {
        applyOspf(virtualRouterUuid, false, completion);
    }

    void getOspfNeighbor(String vrouterUuid, ReturnValueCompletion<List<Neighbor>> completion) {
        GetOspfNeighborCmd cmd = new GetOspfNeighborCmd();

        VirtualRouterAsyncHttpCallMsg vrMsg = new VirtualRouterAsyncHttpCallMsg();
        vrMsg.setVmInstanceUuid(vrouterUuid);
        vrMsg.setPath(ROUTER_PROTOCOL_GET_OSPF_NEIGHBOR);
        vrMsg.setCommand(cmd);
        vrMsg.setCheckStatus(true);
        bus.makeTargetServiceIdByResourceUuid(vrMsg, VmInstanceConstant.SERVICE_ID, vrouterUuid);
        bus.send(vrMsg, new CloudBusCallBack(completion) {
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    VirtualRouterAsyncHttpCallReply ar = reply.castReply();
                    GetOspfNeighborRsp rsp = ar.toResponse(GetOspfNeighborRsp.class);
                    if (rsp.isSuccess()) {
                        completion.success(rsp.neighbors);
                    } else {
                        completion.fail(operr("operation error, because:%s", rsp.getError()));
                    }
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void beforeDetachNic(VmNicInventory nic, Completion completion) {
        /* to be done zhanyong.miao
        check NetworkRouterAreaRef table
        remove all the entries related with this nic in the table
         */
        VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(dbf.findByUuid(nic.getVmInstanceUuid(), VirtualRouterVmVO.class));
        if (!VpcConstants.VPC_VROUTER_VM_TYPE.equals(vrInv.getApplianceVmType())) {
            completion.success();
            return;
        }

        NetworkRouterAreaRefVO ref = Q.New(NetworkRouterAreaRefVO.class).eq(NetworkRouterAreaRefVO_.l3NetworkUuid, nic.getL3NetworkUuid())
                .eq(NetworkRouterAreaRefVO_.vRouterUuid, nic.getVmInstanceUuid()).find();
        if (ref == null) {
            completion.success();
            return;
        }

        dbf.remove(ref);
        applyOspfToAgent(nic.getVmInstanceUuid(), completion);
    }

    @Override
    public void beforeDetachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        completion.done();
    }

    @Override
    public List<VirtualRouterHaCallbackStruct> getCallback() {
        List<VirtualRouterHaCallbackStruct> structs = new ArrayList<>();

        VirtualRouterHaCallbackStruct applyOspf = new VirtualRouterHaCallbackStruct();
        applyOspf.type = APPLY_OSPF_TASK;
        applyOspf.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                applyOspfToAgent(vrUuid, completion);
            }
        };
        structs.add(applyOspf);

        return structs;
    }
}
