package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.header.network.l2.PrepareL2NetworkOnHostMsg;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.service.NetworkServiceConstants;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by weiwang on 19/04/2017.
 */
public class InstantiateVxlanNetworkForNewCreatedVmExtension implements PreVmInstantiateResourceExtensionPoint {
    private static final CLogger logger = Utils.getLogger(InstantiateVxlanNetworkForNewCreatedVmExtension.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {
    }

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        Set<String> vxlanUuids = new HashSet<>();
        for (L3NetworkInventory l3 : VmNicSpec.getL3NetworkInventoryOfSpec(spec.getL3Networks())) {
            String type = Q.New(L2NetworkVO.class).select(L2NetworkVO_.type).eq(L2NetworkVO_.uuid, l3.getL2NetworkUuid()).findValue();
            if (type.equals(VxlanNetworkConstant.VXLAN_NETWORK_TYPE)) {
                vxlanUuids.add(l3.getL2NetworkUuid());
            }
        }

        if (vxlanUuids.isEmpty()) {
            completion.success();
            return;
        }

        ErrorCodeList errList = new ErrorCodeList();

        new While<>(vxlanUuids).step((uuid, completion1) -> {
            PrepareL2NetworkOnHostMsg msg = new PrepareL2NetworkOnHostMsg();
            msg.setL2NetworkUuid(uuid);
            msg.setHost(spec.getDestHost());
            bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, uuid);
            bus.send(msg, new CloudBusCallBack(completion1) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(reply.getError().toString());
                        errList.getCauses().add(reply.getError());
                    } else {
                        logger.debug(String.format("check and realize vxlan network[uuid: %s] for vm[uuid: %s] successed", uuid, spec.getVmInventory().getUuid()));
                    }
                    completion1.done();

                }
            });
        }, 5).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errList.getCauses().isEmpty()) {
                    completion.fail(errList.getCauses().get(0));
                    return;
                }
                logger.info(String.format("check and realize vxlan networks[uuid: %s] for vm[uuid: %s] done", vxlanUuids, spec.getVmInventory().getUuid()));
                completion.success();
            }
        });
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        // TODO(WeiW): Need check and cleanup bridge in host
        completion.success();
    }
}