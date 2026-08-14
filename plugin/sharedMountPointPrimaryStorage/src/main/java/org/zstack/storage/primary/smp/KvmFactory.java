package org.zstack.storage.primary.smp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.kvm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

/**
 * Created by xing5 on 2016/3/26.
 */
public class KvmFactory implements HypervisorFactory, KVMHostConnectExtensionPoint, KVMStartVmExtensionPoint {
    private static final CLogger logger = Utils.getLogger(KvmFactory.class);
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public String getHypervisorType() {
        return KVMConstant.KVM_HYPERVISOR_TYPE;
    }

    @Override
    public HypervisorBackend getHypervisorBackend(PrimaryStorageVO vo) {
        return new KvmBackend(vo);
    }


    @Transactional(readOnly = true)
    private PrimaryStorageInventory findSMPByHostUuid(String clusterUuid) {
        List<PrimaryStorageVO> ret = SQL.New("select pri from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref" +
                " where pri.uuid = ref.primaryStorageUuid" +
                " and ref.clusterUuid = :cuuid" +
                " and pri.type = :ptype", PrimaryStorageVO.class)
                .param("cuuid", clusterUuid)
                .param("ptype", SMPConstants.SMP_TYPE)
                .limit(1)
                .list();
        return ret.isEmpty() ? null : PrimaryStorageInventory.valueOf(ret.get(0));
    }

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __init__ = "init-smp-primary-storage";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                PrimaryStorageInventory ps = findSMPByHostUuid(context.getInventory().getClusterUuid());
                if (ps == null) {
                    trigger.next();
                    return;
                }

                InitKvmHostMsg msg = new InitKvmHostMsg();
                msg.setHypervisorType(context.getInventory().getHypervisorType());
                msg.setHostUuid(context.getInventory().getUuid());
                msg.setPrimaryStorageUuid(ps.getUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ps.getUuid());
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        SMPRecalculatePrimaryStorageCapacityMsg msg = new SMPRecalculatePrimaryStorageCapacityMsg();
                        msg.setPrimaryStorageUuid(ps.getUuid());
                        msg.setRelease(false);
                        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ps.getUuid());
                        bus.send(msg);
                        trigger.next();
                    }
                });
            }
        };
    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (spec.getMemorySnapshotUuid() == null) {
            return;
        }

        VolumeSnapshotVO vo = dbf.findByUuid(spec.getMemorySnapshotUuid(), VolumeSnapshotVO.class);
        if (!Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.type, SMPConstants.SMP_TYPE)
                .eq(PrimaryStorageVO_.uuid, vo.getPrimaryStorageUuid()).isExists()) {
            return;
        }

        cmd.setMemorySnapshotPath(vo.getPrimaryStorageInstallPath());
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }
}
