package org.zstack.compute.vmscheduling;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.SQL;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.HostAllocatorReserveExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.MigrateVmHostReserveRollbackExtensionPoint;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vmscheduling.VmSchedulingRuleConstants;
import org.zstack.header.vmscheduling.VmSchedulingRuleReserveMsg;
import org.zstack.header.vmscheduling.VmSchedulingRuleReserveReply;
import org.zstack.header.vmscheduling.VmSchedulingRuleRollbackMsg;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 * @Author: DaoDao
 * @Date: 2022/12/5
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmSchedulingRuleGroupReserveFlow implements HostAllocatorReserveExtensionPoint, MigrateVmHostReserveRollbackExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmSchedulingRuleGroupReserveFlow.class);

    @Autowired
    private VmSchedulingRuleFilterFlow ruleFilterFlow;
    @Autowired
    private CloudBus bus;

    @Override
    public HostAllocatorReserveExtensionPoint getExtension() {
        return new VmSchedulingRuleGroupReserveFlow();
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        HostInventory host = (HostInventory) data.get(HostAllocatorConstant.Param.HOST);
        HostAllocatorSpec spec = (HostAllocatorSpec) data.get(HostAllocatorConstant.Param.SPEC);
        String groupUuid = ruleFilterFlow.getVmSchedulingRuleGroupUuid(spec);

        if (groupUuid == null) {
            trigger.next();
            return;
        }

        VmSchedulingRuleReserveMsg msg = new VmSchedulingRuleReserveMsg();
        msg.setHost(host);
        msg.setSpec(spec);
        msg.setVmGroupUuid(groupUuid);
        /* use const string as uuid to make all VmSchedulingRuleReserveMsg is handled by same management node */
        bus.makeTargetServiceIdByResourceUuid(msg, VmSchedulingRuleConstants.SERVICE_ID, VmSchedulingRuleConstants.VM_SCHEDULING_RULE_GROUP_RESERVE_ID);
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    data.put(VmSchedulingRuleConstants.Param.RESERVE_SUCCESS, false);
                    trigger.fail(reply.getError());
                } else {
                    VmSchedulingRuleReserveReply rep = (VmSchedulingRuleReserveReply)reply;
                    data.put(VmSchedulingRuleConstants.Param.RESERVE_SUCCESS, true);
                    data.put(VmSchedulingRuleConstants.Param.ORIGIN_HOST_UUID, rep.getOriginHostUuid());
                    trigger.next();
                }
            }
        });

    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        HostInventory host = (HostInventory) data.get(HostAllocatorConstant.Param.HOST);
        HostAllocatorSpec spec = (HostAllocatorSpec) data.get(HostAllocatorConstant.Param.SPEC);
        String vmGroupUuid = ruleFilterFlow.getVmSchedulingRuleGroupUuid(spec);
        boolean success = (boolean)data.get(VmSchedulingRuleConstants.Param.RESERVE_SUCCESS);

        if (vmGroupUuid == null || !success) {
            trigger.rollback();
            return;
        }

        VmSchedulingRuleRollbackMsg msg = new VmSchedulingRuleRollbackMsg();
        msg.setHost(host);
        msg.setSpec(spec);
        msg.setVmGroupUuid(vmGroupUuid);
        msg.setOriginHostUuid((String)data.get(VmSchedulingRuleConstants.Param.ORIGIN_HOST_UUID));
        bus.makeTargetServiceIdByResourceUuid(msg, VmSchedulingRuleConstants.SERVICE_ID, VmSchedulingRuleConstants.VM_SCHEDULING_RULE_GROUP_RESERVE_ID);
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                trigger.rollback();
            }
        });
    }

    @Override
    public void rollbackAfterFailedMigration(VmInstanceSpec vmSpec, Completion completion) {
        HostAllocatorSpec allocatorSpec = new HostAllocatorSpec();
        allocatorSpec.setVmInstance(vmSpec.getVmInventory());
        String groupUuid = ruleFilterFlow.getVmSchedulingRuleGroupUuid(allocatorSpec);
        if (groupUuid == null) {
            completion.success();
            return;
        }

        String originHostUuid = vmSpec.getVmInventory().getHostUuid();
        VmSchedulingRuleRollbackMsg msg = new VmSchedulingRuleRollbackMsg();
        msg.setHost(vmSpec.getDestHost());
        msg.setSpec(allocatorSpec);
        msg.setVmGroupUuid(groupUuid);
        msg.setOriginHostUuid(originHostUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, VmSchedulingRuleConstants.SERVICE_ID, VmSchedulingRuleConstants.VM_SCHEDULING_RULE_GROUP_RESERVE_ID);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to rollback vm[uuid:%s] hostUuid to %s after migration failure, %s; restore it locally",
                            vmSpec.getVmInventory().getUuid(), originHostUuid, reply.getError()));
                    SQL.New(VmInstanceVO.class)
                            .eq(VmInstanceVO_.uuid, vmSpec.getVmInventory().getUuid())
                            .set(VmInstanceVO_.hostUuid, originHostUuid)
                            .update();
                }
                completion.success();
            }
        });
    }
}
