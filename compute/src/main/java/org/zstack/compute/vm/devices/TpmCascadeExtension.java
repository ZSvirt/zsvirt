package org.zstack.compute.vm.devices;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.tpm.entity.TpmVO_;
import org.zstack.header.tpm.message.TpmDeletionMsg;
import org.zstack.header.vm.VmDeletionStruct;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.core.Platform.operr;
import static org.zstack.header.tpm.TpmConstants.SERVICE_ID;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

public class TpmCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(TpmCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private static final String NAME = TpmVO.class.getSimpleName();

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            handleDeletionCheck(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)
                || action.isActionCode(CascadeConstant.VM_INSTANCE_EXPUNGE_CODE)) {
            if (shouldDeferVmAssociatedDeletion(action)) {
                completion.success();
                return;
            }
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            handleDeletionCleanup(action, completion);
        } else {
            completion.success();
        }
    }

    /**
     * When VM deletion policy keeps the VM row (recycle bin), defer TPM removal until {@link CascadeConstant#VM_INSTANCE_EXPUNGE_CODE}.
     */
    private boolean shouldDeferVmAssociatedDeletion(CascadeAction action) {
        if (CascadeConstant.VM_INSTANCE_EXPUNGE_CODE.equals(action.getActionCode())) {
            return false;
        }
        if (!VmInstanceVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return false;
        }
        Object raw = action.getParentIssuerContext();
        if (!(raw instanceof List)) {
            return false;
        }
        for (Object o : (List<?>) raw) {
            if (!(o instanceof VmDeletionStruct)) {
                continue;
            }
            VmDeletionStruct s = (VmDeletionStruct) o;
            VmInstanceDeletionPolicy p = s.getDeletionPolicy();
            if (p == VmInstanceDeletionPolicy.Delay || p == VmInstanceDeletionPolicy.Never) {
                return true;
            }
        }
        return false;
    }

    private List<TpmVO> tpmFromAction(CascadeAction action) {
        if (VmInstanceVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<VmDeletionStruct> vmDeletionStructs = action.getParentIssuerContext();
            List<String> vmUuidList = transformAndRemoveNull(vmDeletionStructs, it -> it.getInventory().getUuid());

            if (vmUuidList.isEmpty()) {
                return null;
            }
            return Q.New(TpmVO.class)
                    .in(TpmVO_.vmInstanceUuid, vmUuidList)
                    .list();
        } else if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        }

        return null;
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    private void handleDeletion(CascadeAction action, Completion completion) {
        final List<TpmVO> tpmList = tpmFromAction(action);
        if (CollectionUtils.isEmpty(tpmList)) {
            completion.success();
            return;
        }

        new While<>(tpmList).each((tpm, whileCompletion) -> {
            TpmDeletionMsg msg = new TpmDeletionMsg();
            msg.setTpmUuid(tpm.getUuid());
            msg.setVmInstanceUuid(tpm.getVmInstanceUuid());
            // delete TPM in cascade must skip VM state checking -> force should always BE TRUE
            msg.setForceDelete(true);
            bus.makeTargetServiceIdByResourceUuid(msg, SERVICE_ID, msg.getTpmUuid());
            bus.send(msg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        logger.debug(String.format("deleted Tpm[uuid:%s] from VM[uuid:%s]",
                                tpm.getUuid(), tpm.getVmInstanceUuid()));
                        whileCompletion.done();
                    } else {
                        whileCompletion.addError(reply.getError());
                        whileCompletion.allDone();
                    }
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.isEmpty()) {
                    completion.fail(operr("failed to delete Tpm from VM[uuid:%s]", tpmList.get(0).getVmInstanceUuid())
                            .withCause(errorCodeList));
                    return;
                }
                completion.success();
            }
        });
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return list(VmInstanceVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())
                || CascadeConstant.VM_INSTANCE_EXPUNGE_CODE.equals(action.getActionCode())) {
            List<TpmVO> ctx = tpmFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }

        return null;
    }
}
