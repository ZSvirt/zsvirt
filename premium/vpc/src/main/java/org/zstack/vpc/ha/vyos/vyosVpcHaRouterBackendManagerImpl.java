package org.zstack.vpc.ha.vyos;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vpc.VpcRouterVmVO;
import org.zstack.header.vpc.ha.VpcHaGroupMonitorIpVO;
import org.zstack.header.vpc.ha.VpcHaGroupVO;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
import org.zstack.network.service.virtualrouter.VirtualRouterAsyncHttpCallMsg;
import org.zstack.network.service.virtualrouter.VirtualRouterAsyncHttpCallReply;
import org.zstack.network.service.virtualrouter.VirtualRouterNicMetaData;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.vpc.ha.VpcHaGroupGlobalConfig;
import org.zstack.vpc.ha.VpcHaGroupOperator;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.operr;

public class vyosVpcHaRouterBackendManagerImpl implements vyosVpcHaRouterBackendManager {
    private final static CLogger logger = Utils.getLogger(vyosVpcHaRouterBackendManagerImpl.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private RESTFacade restf;


    @Override
    public void enableHa(String vrUuid, Completion completion) {
        enableHa(vrUuid, null, completion);
    }

    @Override
    public void enableHa(String vrUuid, Long timeout, Completion completion) {
        VpcRouterVmVO vpc = dbf.findByUuid(vrUuid, VpcRouterVmVO.class);
        if (vpc == null || !vpc.isHaEnabled()) {
            completion.success();
            return;
        }

        String VpcHaGroupUuid = VpcHaGroupOperator.getVpcHaGroupUuid(vrUuid);
        if (VpcHaGroupUuid == null) {
            completion.success();
            return;
        }

        VpcHaGroupVO vpcHa = dbf.findByUuid(VpcHaGroupUuid, VpcHaGroupVO.class);
        VpcHaRouterCommands.VyosHaEnableCmd cmd = new VpcHaRouterCommands.VyosHaEnableCmd();
        cmd.keepalive = rcf.getResourceConfigValue(VpcHaGroupGlobalConfig.KEEPALIVED_INTERVAL, vpcHa.getUuid(), Integer.class);

        for (VmNicVO nic : vpc.getVmNics()) {
            /* if heartbeat network is not specified, use mgt network */
            if (VirtualRouterNicMetaData.isManagementNic(nic)) {
                cmd.heartbeatNic = nic.getMac();
                cmd.localIp = nic.getIp();
                break;
            }
        }

        String peerUuid = VpcHaGroupOperator.getVpcHaRouterPeerUuid(vrUuid);
        if (peerUuid != null) {
            VpcRouterVmVO peer = dbf.findByUuid(peerUuid, VpcRouterVmVO.class);
            for (VmNicVO nic : peer.getVmNics()) {
                if (VirtualRouterNicMetaData.isManagementNic(nic)) {
                    cmd.peerIp = nic.getIp();
                    break;
                }
            }
        }

        List<String> monitors = new ArrayList<>();
        for (VpcHaGroupMonitorIpVO ip : vpcHa.getMonitors()) {
            monitors.add(ip.getMonitorIp());
        }
        cmd.setMonitors(monitors);

        /* add public vip */
        List<VpcHaRouterCommands.VyosHaVip> vipInfos = new ArrayList<>();
        List<String> vipUuids = new VpcHaGroupOperator().getVpcHaGroupSystemVipUuids(VpcHaGroupUuid);
        List<VipVO> vips = new ArrayList<>();
        if (!vipUuids.isEmpty()) {
            vips = Q.New(VipVO.class).in(VipVO_.uuid, vipUuids).list();
        }
        for (VipVO vip : vips) {
            /* TODO: this is a workaround method to skip ipv6 vip */
            if (!NetworkUtils.isIpv4Address(vip.getIp())) {
                continue;
            }
            for (VmNicVO nic : vpc.getVmNics()) {
                if (vip.getL3NetworkUuid().equals(nic.getL3NetworkUuid())) {
                    VpcHaRouterCommands.VyosHaVip vipInfo = new VpcHaRouterCommands.VyosHaVip();
                    vipInfo.nicMac = nic.getMac();
                    vipInfo.nicVip = vip.getIp();
                    vipInfo.netmask = nic.getNetmask();
                    vipInfos.add(vipInfo);
                }
            }
        }
        cmd.setVips(vipInfos);
        cmd.callbackUrl = restf.getSendCommandUrl();

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setPath(VpcHaRouterCommands.VYOS_HA_ENABLE_PATH);
        msg.setCommand(cmd);
        msg.setVmInstanceUuid(vrUuid);
        if (timeout != null) {
            msg.setTimeout(timeout);
        }
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vrUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                VpcHaRouterCommands.VyosHaEnableRsp ret = re.toResponse(VpcHaRouterCommands.VyosHaEnableRsp.class);
                if (!ret.isSuccess()) {
                    ErrorCode err = operr("failed to enable ha on virtual router[uuid:%s], %s",
                            vrUuid, ret.getError());
                    completion.fail(err);
                } else {
                    logger.debug(String.format("enable ha on virtual router[uuid:%s] successfully", vrUuid));
                    completion.success();
                }
            }
        });
    }
}
