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
import org.zstack.header.vm.VmDeletionStruct;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.additions.VmHostBackupFileDeletionMsg;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO_;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

public class VmHostBackupFileCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(VmHostBackupFileCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private static final String NAME = VmHostBackupFileVO.class.getSimpleName();

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            handleDeletionCheck(action, completion);
        } else if (action.isActionCode(CascadeConstant.VM_INSTANCE_EXPUNGE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
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

    private boolean shouldDeferVmAssociatedDeletion(CascadeAction action) {
        if (CascadeConstant.VM_INSTANCE_EXPUNGE_CODE.equals(action.getActionCode())) {
            return false;
        }
        if (!VmInstanceVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return false;
        }
        if (hasCreatedVmInDeletionContext(action)) {
            logger.info(String.format(
                    "VmHostBackupFileCascadeExtension: skip deferring backup-file deletion for Created VM(s): %s; "
                            + "destroy uses DBOnly hard-delete without expunge, backup rows must be removed in this cascade",
                    formatCreatedVmUuidsFromContext(action)));
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

    private boolean hasCreatedVmInDeletionContext(CascadeAction action) {
        Object raw = action.getParentIssuerContext();
        if (!(raw instanceof List)) {
            return false;
        }
        for (Object o : (List<?>) raw) {
            if (!(o instanceof VmDeletionStruct)) {
                continue;
            }
            VmInstanceInventory inv = ((VmDeletionStruct) o).getInventory();
            if (inv != null && VmInstanceState.Created.toString().equals(inv.getState())) {
                return true;
            }
        }
        return false;
    }

    private String formatCreatedVmUuidsFromContext(CascadeAction action) {
        Object raw = action.getParentIssuerContext();
        if (!(raw instanceof List)) {
            return "[]";
        }
        return ((List<?>) raw).stream()
                .filter(VmDeletionStruct.class::isInstance)
                .map(VmDeletionStruct.class::cast)
                .map(VmDeletionStruct::getInventory)
                .filter(inv -> inv != null && VmInstanceState.Created.toString().equals(inv.getState()))
                .map(VmInstanceInventory::getUuid)
                .collect(Collectors.joining(", "));
    }

    private List<VmHostBackupFileVO> voFromAction(CascadeAction action) {
        if (VmInstanceVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<VmDeletionStruct> vmDeletionStructs = action.getParentIssuerContext();
            List<String> vmUuidList = transformAndRemoveNull(vmDeletionStructs, it -> it.getInventory().getUuid());

            if (vmUuidList.isEmpty()) {
                return null;
            }
            return Q.New(VmHostBackupFileVO.class)
                    .in(VmHostBackupFileVO_.resourceUuid, vmUuidList)
                    .list();
        }
        // Note: VolumeSnapshotGroupVO has no cascade extension!
        // skip: else if (VolumeSnapshotGroupVO.class.getSimpleName().equals(action.getParentIssuer()))
        else if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        }

        return null;
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    private void handleDeletion(CascadeAction action, Completion completion) {
        final List<VmHostBackupFileVO> voList = voFromAction(action);
        if (CollectionUtils.isEmpty(voList)) {
            completion.success();
            return;
        }

        Set<String> createdVmUuidsAsResource = findVmUuidsInCreatedState(
                voList.stream().map(VmHostBackupFileVO::getResourceUuid).collect(Collectors.toSet()));
        if (!createdVmUuidsAsResource.isEmpty()) {
            logger.info(String.format(
                    "VmHostBackupFileCascadeExtension: deleting VmHostBackupFile row(s) tied to Created VM(s) %s with forceDelete=true "
                            + "(same as expunge path; avoids leftovers when VM row is hard-deleted)",
                    String.join(", ", createdVmUuidsAsResource)));
        }

        new While<>(voList).each((vo, whileCompletion) -> {
            VmHostBackupFileDeletionMsg msg = new VmHostBackupFileDeletionMsg();
            msg.setUuid(vo.getUuid());
            boolean force = action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE)
                    || CascadeConstant.VM_INSTANCE_EXPUNGE_CODE.equals(action.getActionCode())
                    || createdVmUuidsAsResource.contains(vo.getResourceUuid());
            msg.setForceDelete(force);
            bus.makeLocalServiceId(msg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
            bus.send(msg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        logger.debug(String.format("deleted VmHostBackupFile[uuid:%s] from resource[uuid:%s]",
                                vo.getUuid(), vo.getResourceUuid()));
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
                    completion.fail(operr("failed to delete VmHostBackupFile from resource[uuid:%s]", voList.get(0).getResourceUuid())
                            .withCause(errorCodeList));
                    return;
                }
                completion.success();
            }
        });
    }

    private Set<String> findVmUuidsInCreatedState(Set<String> candidateVmUuids) {
        if (candidateVmUuids.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(Q.New(VmInstanceVO.class)
                .in(VmInstanceVO_.uuid, candidateVmUuids)
                .eq(VmInstanceVO_.state, VmInstanceState.Created)
                .select(VmInstanceVO_.uuid)
                .listValues());
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return list(
            VmInstanceVO.class.getSimpleName()

            // Note: VolumeSnapshotGroupVO has no cascade extension!
            // skip: VolumeSnapshotGroupVO.class.getName()
        );
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())
                || CascadeConstant.VM_INSTANCE_EXPUNGE_CODE.equals(action.getActionCode())) {
            List<VmHostBackupFileVO> ctx = voFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }

        return null;
    }
}