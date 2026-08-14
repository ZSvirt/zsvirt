package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.network.l3.ReturnIpMsg;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmReturnReleaseNicFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VmReturnReleaseNicFlow.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected VmInstanceDeletionPolicyManager deletionPolicyMgr;

    @Override
    public void run(FlowTrigger chain, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        if (spec.getVmInventory().getVmNics().isEmpty()) {
            chain.next();
            return;
        }

        List<ReturnIpMsg> msgs = new ArrayList<>(spec.getVmInventory().getVmNics().size());
        for (VmNicInventory nic : spec.getVmInventory().getVmNics()) {
            for (UsedIpInventory ip : nic.getUsedIps()) {
                ReturnIpMsg msg = new ReturnIpMsg();
                msg.setL3NetworkUuid(ip.getL3NetworkUuid());
                msg.setUsedIpUuid(ip.getUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, ip.getL3NetworkUuid());
                msgs.add(msg);
            }
        }

        new While<>(msgs).each((returnIpMsg, completion) -> bus.send(returnIpMsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to release ip[usedIpUuid:%s] for vm[uuid:%s], but continue anyway",
                            returnIpMsg.getUsedIpUuid(), spec.getVmInventory().getUuid()));
                }
                completion.done();
            }
        })).run(new WhileDoneCompletion(chain) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                for (VmNicInventory nic : spec.getVmInventory().getVmNics()) {
                    VmNicVO vo = dbf.findByUuid(nic.getUuid(), VmNicVO.class);
                    if (VmInstanceConstant.USER_VM_TYPE.equals(spec.getVmInventory().getType())) {
                        VmInstanceDeletionPolicy deletionPolicy = getDeletionPolicy(spec, data);
                        if (deletionPolicy == VmInstanceDeletionPolicy.Direct) {
                            dbf.remove(vo);
                        } else {
                            vo.removeIp();
                            dbf.update(vo);
                        }
                    } else {
                        dbf.remove(vo);
                    }
                }
                chain.next();
            }
        });
    }

    private VmInstanceDeletionPolicy getDeletionPolicy(VmInstanceSpec spec, Map data) {
        if (data.containsKey(VmInstanceConstant.Params.DeletionPolicy)) {
            return (VmInstanceDeletionPolicy) data.get(VmInstanceConstant.Params.DeletionPolicy);
        }

        return deletionPolicyMgr.getDeletionPolicy(spec.getVmInventory().getUuid());
    }
}
