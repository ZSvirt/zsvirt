package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

/**
 * Created by shixin.ruan on 18-03-10.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VyosChangePrivateL3FirewallDefaultActionFlow extends NoRollbackFlow {
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ApiTimeoutManager apiTimeoutManager;

    private final static CLogger logger = Utils.getLogger(VyosChangePrivateL3FirewallDefaultActionFlow.class);

    @Override
    public void run(FlowTrigger trigger, Map data) {
        String action = VyosGlobalConfig.PRIVATE_L3_FIREWALL_DEFAULT_ACTION.value(String.class);

        final VirtualRouterVmInventory servedVm = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
        List<VirtualRouterCommands.NicInfo> infos = CollectionUtils.transformAndRemoveNull(servedVm.getGuestNics(), arg -> {
            VirtualRouterCommands.NicInfo info = new VirtualRouterCommands.NicInfo();
            info.setIp(arg.getIp());
            info.setDefaultRoute(false);
            info.setGateway(arg.getGateway());
            info.setMac(arg.getMac());
            info.setNetmask(arg.getNetmask());
            info.setFirewallDefaultAction(action);
            return info;
        });

        if (infos == null || infos.isEmpty()) {
            trigger.next();
            return;
        }

        VirtualRouterCommands.ConfigureNicFirewallDefaultActionCmd cmd = new VirtualRouterCommands.ConfigureNicFirewallDefaultActionCmd();
        cmd.setNics(infos);

        VirtualRouterAsyncHttpCallMsg cmsg = new VirtualRouterAsyncHttpCallMsg();
        cmsg.setCommand(cmd);
        cmsg.setPath(VirtualRouterConstant.VR_CONFIGURE_NIC_FIREWALL_DEFAULT_ACTION_PATH);
        cmsg.setVmInstanceUuid(servedVm.getUuid());
        bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, servedVm.getUuid());
        bus.send(cmsg, new CloudBusCallBack(trigger) {
            /* failure in this flow will not block normal process */
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.debug(String.format("failed to change nic firewall default action of virtual router vm[uuid:%s ip:%s], because %s",
                            servedVm.getUuid(), servedVm.getManagementNic().getIp(), reply.getError()));
                    trigger.next();
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                VirtualRouterCommands.ConfigureNicFirewallDefaultActionRsp rsp = re.toResponse(VirtualRouterCommands.ConfigureNicFirewallDefaultActionRsp.class);
                if (rsp.isSuccess()) {
                    logger.debug(String.format("successfully change nic firewall default action of virtual router vm[uuid:%s, ip:%s]",
                            servedVm.getUuid(), servedVm.getManagementNic().getIp()));
                    trigger.next();
                } else {
                    logger.debug(String.format("failed to change nic firewall default action of virtual router vm[uuid:%s ip:%s], because %s",
                            servedVm.getUuid(), servedVm.getManagementNic().getIp(), rsp.getError()));
                    trigger.next();
                }
            }
        });
    }
}
