package org.zstack.vpc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.dhcp.VirtualRouterDhcpBackend;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.network.service.virtualrouter.vyos.VyosConstants;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.header.vpc.VpcConstants;
import org.zstack.header.vpc.VpcRouterDnsVO;
import org.zstack.header.vpc.VpcRouterDnsVO_;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.vpc.ha.VpcHaGroupOperator;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.header.vpc.VpcConstants.VR_SET_VPCDNS_PATH;

public class VpcRouterDnsBackend implements VirtualRouterAfterAttachNicExtensionPoint, VirtualRouterBeforeDetachNicExtensionPoint,
        VirtualRouterHaGetCallbackExtensionPoint {
    private final static CLogger logger = Utils.getLogger(VpcManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VpcManager vpcMgr;
    @Autowired
    private VirtualRouterHaBackend haBackend;
    @Autowired
    private NetworkServiceManager nwServiceMgr;
    @Autowired
    @Qualifier("VirtualRouterDhcpBackend")
    private VirtualRouterDhcpBackend dhcpBackend;

    private final String SET_DNS_TASK = "setDns";

    public void applyDnsToVpcRouter(String vrUuid, boolean toHaRouter, Completion completion) {
        applyDnsToVpcRouter(vrUuid, null, toHaRouter, completion);
    }

    private void submitSetDnsToHaRouter(VirtualRouterVmInventory vrInv, String l3Uuid, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(SET_DNS_TASK);
        task.setOriginRouterUuid(vrInv.getUuid());
        task.setJsonData(l3Uuid);
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    public void applyDnsToVpcRouter(String vrUuid, String l3Uuid, boolean toHaRouter, Completion completion) {
        VpcRouterCommands.VpcRouterSetDnsCmd cmd = new VpcRouterCommands.VpcRouterSetDnsCmd();

        List<String> dns = vpcMgr.getAllDnsFromVpcRouter(vrUuid);
        cmd.setDns(dns);

        VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(dbf.findByUuid(vrUuid, VirtualRouterVmVO.class));
        List<String> nicsMac = new ArrayList<>();
        for (VmNicInventory nic: vrInv.getVmNics()) {
            if (nic.getL3NetworkUuid().equals(l3Uuid)) {
                continue;
            }

            if (isDnsNeeded(vrInv, nic)) {
                nicsMac.add(nic.getMac());
            }
        }
        cmd.setNicMac(nicsMac);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setPath(VR_SET_VPCDNS_PATH);
        msg.setCommand(cmd);
        msg.setVmInstanceUuid(vrUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vrUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.debug(String.format("vpc virtual router[uuid: %s] failed to apply dns, because %s",
                            vrUuid, reply.getError()));
                    completion.fail(reply.getError());
                } else {
                    VirtualRouterAsyncHttpCallReply re = reply.castReply();
                    VirtualRouterCommands.RemoveDnsRsp ret = re.toResponse(VirtualRouterCommands.RemoveDnsRsp.class);
                    if (ret.isSuccess()) {
                        logger.debug(String.format("vpc virtual router[uuid: %s] successfully apply dns",
                                vrUuid));
                        if (!toHaRouter) {
                            completion.success();
                            return;
                        }

                        submitSetDnsToHaRouter(vrInv, l3Uuid, completion);
                    } else {
                        logger.debug(String.format("vpc virtual router[uuid: %s] failed to apply dns, because %s",
                                vrUuid, ret.getError()));
                        completion.fail(reply.getError());
                    }
                }
            }
        });
    }

    private boolean isDnsNeeded(VirtualRouterVmInventory vrInv, VmNicInventory nic) {
        if (VirtualRouterNicMetaData.isGuestNic(nic)) {
            return true;
        }

        if (VirtualRouterNicMetaData.isManagementNic(nic) && !VirtualRouterNicMetaData.isPublicNic(nic)) {
            return false;
        }

        try {
            final NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(nic.getL3NetworkUuid(),
                    NetworkServiceType.DHCP);
            if (providerType != VyosConstants.PROVIDER_TYPE) {
                return false;
            }

            /* if vpc is dhcp router for nic.l3NetworkUuid, then dns is need to be enabled on this network */
            L3NetworkInventory l3Nw = L3NetworkInventory.valueOf(dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class));
            VirtualRouterVmInventory dhcpRouter = dhcpBackend.getVirtualRouterForVyosDhcp(l3Nw);
            if (dhcpRouter == null) {
                return false;
            }

            if (dhcpRouter.getUuid().equals(nic.getVmInstanceUuid())) {
                return true;
            }

            if (vrInv.isHaEnabled()) {
                String peerUuid = VpcHaGroupOperator.getVpcHaRouterPeerUuid(nic.getVmInstanceUuid());
                return dhcpRouter.getUuid().equals(peerUuid);
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isVpcDnsNotNeeded(VirtualRouterVmInventory vrInv, VmNicInventory nic) {
        return !VpcConstants.VPC_VROUTER_VM_TYPE.equals(vrInv.getApplianceVmType()) || !isDnsNeeded(vrInv, nic);
    }

    @Override
    public void afterAttachNic(VmNicInventory nic, Completion completion) {
        VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(dbf.findByUuid(nic.getVmInstanceUuid(), VirtualRouterVmVO.class));
        if (isVpcDnsNotNeeded(vrInv, nic)) {
            completion.success();
            return;
        }

        applyDnsToVpcRouter(nic.getVmInstanceUuid(), false, completion);
    }

    @Override
    public void afterAttachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(dbf.findByUuid(nic.getVmInstanceUuid(), VirtualRouterVmVO.class));
        if (isVpcDnsNotNeeded(vrInv, nic)) {
            completion.done();
            return;
        }

        applyDnsToVpcRouter(nic.getVmInstanceUuid(), nic.getL3NetworkUuid(), false, new Completion(completion) {
            @Override
            public void success() {
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.done();
            }
        });
    }

    @Override
    public void beforeDetachNic(VmNicInventory nic, Completion completion) {
        VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(dbf.findByUuid(nic.getVmInstanceUuid(), VirtualRouterVmVO.class));
        if (isVpcDnsNotNeeded(vrInv, nic)) {
            completion.success();
            return;
        }

        applyDnsToVpcRouter(nic.getVmInstanceUuid(), nic.getL3NetworkUuid(), false, completion);
    }

    @Override
    public void beforeDetachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(dbf.findByUuid(nic.getVmInstanceUuid(), VirtualRouterVmVO.class));
        if (isVpcDnsNotNeeded(vrInv, nic)) {
            completion.done();
            return;
        }

        applyDnsToVpcRouter(nic.getVmInstanceUuid(), false, new Completion(completion) {
            @Override
            public void success() {
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.done();
            }
        });
    }

    @Override
    public List<VirtualRouterHaCallbackStruct> getCallback() {
        List<VirtualRouterHaCallbackStruct> structs = new ArrayList<>();

        VirtualRouterHaCallbackStruct setDns = new VirtualRouterHaCallbackStruct();
        setDns.type = SET_DNS_TASK;
        setDns.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                boolean exists = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, vrUuid).isExists();
                if (!exists) {
                    logger.debug(String.format("ha router[%s] does not existed, can not call create setDns", vrUuid));
                    completion.success();
                    return;
                }

                String l3NetworkUuid = task.getJsonData();
                applyDnsToVpcRouter(vrUuid, l3NetworkUuid, false, completion);
            }
        };
        structs.add(setDns);

        return structs;
    }
}
