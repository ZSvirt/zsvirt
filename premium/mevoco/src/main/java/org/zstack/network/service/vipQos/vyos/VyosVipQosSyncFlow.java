package org.zstack.network.service.vipQos.vyos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vipQos.*;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.network.service.virtualrouter.vip.VipConfigProxy;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by liangbo.zhou on 17-6-12.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VyosVipQosSyncFlow extends NoRollbackFlow {
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected VirtualRouterHaBackend haBackend;
    @Autowired
    protected VipConfigProxy vipProxy;

    private final static CLogger logger = Utils.getLogger(VyosVipQosSyncFlow.class);

    @Override
    public void run(FlowTrigger trigger, Map data) {
        final VirtualRouterVmInventory servedVm = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());

        List<String> vipUuids = vipProxy.getServiceUuidsByRouterUuid(servedVm.getUuid(), VipVO.class.getSimpleName());
        if (vipUuids == null || vipUuids.isEmpty()) {
            trigger.next();
            return;
        }

        vipUuids = vipUuids.stream().filter(vipUuid -> Q.New(VipQosVO.class).eq(VipQosVO_.vipUuid, vipUuid).count() > 0)
                .collect(Collectors.toList());

        List<ErrorCode> errs = new ArrayList<>();
        new While<>(vipUuids).each((vipUuid, compl) -> {
            SyncVipQosMsg syncMsg = new SyncVipQosMsg();
            syncMsg.setVrUuid(servedVm.getUuid());
            syncMsg.setVipUuid(vipUuid);
            bus.makeTargetServiceIdByResourceUuid(syncMsg, VipQosConstants.SERVICE_ID, vipUuid);
            bus.send(syncMsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        errs.add(reply.getError());
                    }
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if(!errs.isEmpty()){
                    trigger.fail(errs.get(0));
                }else {
                    trigger.next();;
                }
            }
        });
    }
}
