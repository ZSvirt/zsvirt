package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.service.VirtualRouterAfterAttachNicExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import static org.zstack.core.Platform.operr;

public class VyosChangePrivateL3FirewallDefaultActionExtensionPoint implements VirtualRouterAfterAttachNicExtensionPoint {
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ApiTimeoutManager apiTimeoutManager;
    private final static CLogger logger = Utils.getLogger(VyosChangePrivateL3FirewallDefaultActionExtensionPoint.class);

    @Override
    public void afterAttachNic(VmNicInventory nic, Completion completion) {
        if (!VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST.contains(nic.getMetaData())) {
            completion.success();
            return;
        }

        String action = VyosGlobalConfig.PRIVATE_L3_FIREWALL_DEFAULT_ACTION.value(String.class);
        VirtualRouterCommands.NicInfo info = new VirtualRouterCommands.NicInfo();
        info.setIp(nic.getIp());
        info.setDefaultRoute(false);
        info.setGateway(nic.getGateway());
        info.setMac(nic.getMac());
        info.setNetmask(nic.getNetmask());
        info.setFirewallDefaultAction(action);

        VirtualRouterCommands.ConfigureNicFirewallDefaultActionCmd cmd = new VirtualRouterCommands.ConfigureNicFirewallDefaultActionCmd();
        cmd.setNics(Collections.singletonList(info));

        VirtualRouterAsyncHttpCallMsg cmsg = new VirtualRouterAsyncHttpCallMsg();
        cmsg.setCommand(cmd);
        cmsg.setPath(VirtualRouterConstant.VR_CONFIGURE_NIC_FIREWALL_DEFAULT_ACTION_PATH);
        cmsg.setVmInstanceUuid(nic.getVmInstanceUuid());
        bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, nic.getVmInstanceUuid());
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                VirtualRouterCommands.ConfigureNicFirewallDefaultActionRsp rsp = re.toResponse(VirtualRouterCommands.ConfigureNicFirewallDefaultActionRsp.class);
                if (rsp.isSuccess()) {
                    logger.debug(String.format("successfully change nic[ip:%s, mac:%s] firewall default action of virtual router vm[uuid:%s]",
                            nic.getIp(), nic.getMac(), nic.getVmInstanceUuid()));
                    completion.success();
                } else {
                    ErrorCode err = operr("failed to change nic[ip:%s, mac:%s] firewall default action of virtual router vm[uuid:%s], because %s",
                            nic.getIp(), nic.getMac(), nic.getVmInstanceUuid(), rsp.getError());
                    completion.fail(err);
                }
            }
        });
    }

    @Override
    public void afterAttachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        /* rollback nic will delete all nic configure */
        completion.done();
    }
}
