package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.DetachDataVolumeFromHostMsg;
import org.zstack.header.host.HostConstant;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageDetachStruct;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.*;
import org.zstack.header.volume.VolumeDeletionPolicyManager.VolumeDeletionPolicy;
import org.zstack.identity.ResourceHelper;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 */
public class VolumeCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(VolumeCascadeExtension.class);
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private VolumeDeletionPolicyManager deletionPolicyManager;

    private static final String NAME = VolumeVO.class.getSimpleName();

    private static final int OP_DELETE_VOLUME = 0;
    private static final int OP_UPDATE_DISK_OFFERING_COLUMN = 1;

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            handleDeletionCheck(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            handleDeletionCleanup(action, completion);
        } else if (action.isActionCode(PrimaryStorageConstant.PRIMARY_STORAGE_DETACH_CODE)) {
            handlePrimaryStorageDetach(action, completion);
        } else {
            completion.success();
        }
    }

    private int actionToOpCode(CascadeAction action) {
        if (action.getParentIssuer().equals(NAME) || action.getParentIssuer().equals(PrimaryStorageVO.class.getSimpleName())) {
            return OP_DELETE_VOLUME;
        }

        if (action.getParentIssuer().equals(DiskOfferingVO.class.getSimpleName())) {
            return OP_UPDATE_DISK_OFFERING_COLUMN;
        }

        if (action.getParentIssuer().equals(AccountVO.class.getSimpleName())) {
            return OP_DELETE_VOLUME;
        }

        throw new CloudRuntimeException(String.format("unknown edge [%s]", action.getParentIssuer()));
    }

    private String actionToDeletionPolicy(CascadeAction action, String volUuid) {
        if (action.getParentIssuer().equals(PrimaryStorageVO.class.getSimpleName())) {
            return VolumeDeletionPolicy.DBOnly.toString();
        } else {
            return deletionPolicyManager.getDeletionPolicy(volUuid).toString();
        }
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        int op = actionToOpCode(action);
        if (op != OP_DELETE_VOLUME) {
            completion.success();
            return;
        }

        List<String> volumeUuids = volumesCleanupFromAction(action);
        if (volumeUuids == null || volumeUuids.isEmpty()) {
            completion.success();
            return;
        }

        for (String volumeUuid  : volumeUuids) {
            dbf.eoCleanup(VolumeVO.class, volumeUuid);
        }
        completion.success();
    }

    private List<String> volumesCleanupFromAction(CascadeAction action) {
        List<String> volumeUuids = new ArrayList<>();
        if (NAME.equals(action.getParentIssuer())) {
            List<VolumeDeletionStruct> list = action.getParentIssuerContext();
            if (list == null) {
                return null;
            }

            volumeUuids = transformAndRemoveNull(list, arg -> arg.getInventory().getUuid());
            return volumeUuids;
        } else if (PrimaryStorageVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<PrimaryStorageInventory> pinvs = action.getParentIssuerContext();
            if (pinvs == null || pinvs.isEmpty()) {
                return null;
            }

            List<String> psUuids = CollectionUtils.transform(pinvs, PrimaryStorageInventory::getUuid);

            volumeUuids = Q.New(VolumeEO.class)
                    .in(VolumeAO_.primaryStorageUuid, psUuids)
                    .select(VolumeAO_.uuid)
                    .listValues();
            return volumeUuids;
        } else if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            final List<String> auuids = CollectionUtils.transform(action.getParentIssuerContext(), AccountInventory::getUuid);
            if (CollectionUtils.isEmpty(auuids)) {
                return null;
            }

            volumeUuids = ResourceHelper.findOwnResourceUuidList(VolumeEO.class, auuids,
                    q -> q.eq(VolumeVO_.type, VolumeType.Data));
            return volumeUuids;
        }

        return null;
    }

    private List<VolumeDeletionStruct> toVolumeDeletionStruct(CascadeAction action, List<VolumeVO> vos) {
        List<VolumeDeletionStruct> structs = new ArrayList<VolumeDeletionStruct>();
        for (VolumeVO vo : vos) {
            VolumeDeletionStruct s = new VolumeDeletionStruct();
            s.setInventory(VolumeInventory.valueOf(vo));
            s.setDeletionPolicy(actionToDeletionPolicy(action, vo.getUuid()));
            structs.add(s);
        }
        return structs;
    }

    private List<VolumeDeletionStruct> volumesFromAction(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            if (NAME.equals(action.getParentIssuer())) {
                return action.getParentIssuerContext();
            } else if (PrimaryStorageVO.class.getSimpleName().equals(action.getParentIssuer())) {
                List<PrimaryStorageInventory> pinvs = action.getParentIssuerContext();
                List<String> psUuids = CollectionUtils.transform(pinvs, PrimaryStorageInventory::getUuid);

                SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
                q.add(VolumeVO_.type, Op.IN, Arrays.asList(VolumeType.Data, VolumeType.Memory));
                q.add(VolumeVO_.primaryStorageUuid, Op.IN, psUuids);
                List<VolumeVO> vos = q.list();
                return toVolumeDeletionStruct(action, vos);
            } else if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
                final List<String> auuids = CollectionUtils.transform(action.getParentIssuerContext(), AccountInventory::getUuid);

                List<VolumeVO> vos = ResourceHelper.findOwnResources(VolumeVO.class, auuids);
                vos.removeIf(volume -> volume.getType() != VolumeType.Data);

                if (!vos.isEmpty()) {
                    return toVolumeDeletionStruct(action, vos);
                }
            }
        }

        return null;
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        int op = actionToOpCode(action);
        
        if (op == OP_DELETE_VOLUME) {
            deleteVolume(action, completion);
        } else if (op == OP_UPDATE_DISK_OFFERING_COLUMN) {
            if (VolumeGlobalConfig.UPDATE_DISK_OFFERING_TO_NULL_WHEN_DELETING.value(Boolean.class)) {
                updateDiskOfferingColumn(action, completion);
            } else {
                completion.success();
            }
        }
    }

    private void updateDiskOfferingColumn(CascadeAction action, Completion completion) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                List<DiskOfferingInventory> diskOfferingInventories = action.getParentIssuerContext();
                sql(VolumeVO.class).set(VolumeVO_.diskOfferingUuid, null)
                        .in(VolumeVO_.uuid, diskOfferingInventories.stream().map(DiskOfferingInventory::getUuid).collect(Collectors.toList()))
                        .update();
            }
        }.execute();

        completion.success();
    }

    private void deleteVolume(final CascadeAction action, final Completion completion) {
        final List<VolumeDeletionStruct> volumes = volumesFromAction(action);
        if (volumes == null || volumes.isEmpty()) {
            completion.success();
            return;
        }

        List<VolumeDeletionMsg> msgs = new ArrayList<VolumeDeletionMsg>();
        for (VolumeDeletionStruct vol : volumes) {
            VolumeDeletionMsg msg = new VolumeDeletionMsg();
            msg.setDetachBeforeDeleting(vol.isDetachBeforeDeleting());
            msg.setForceDelete(action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE));
            msg.setVolumeUuid(vol.getInventory().getUuid());
            msg.setDeletionPolicy(vol.getDeletionPolicy());
            bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, vol.getInventory().getUuid());
            msgs.add(msg);
        }

        bus.send(msgs, 10, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                if (!action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
                    for (MessageReply r : replies) {
                        if (!r.isSuccess()) {
                            completion.fail(r.getError());
                            return;
                        }
                    }
                }

                completion.success();
            }
        }); 
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(PrimaryStorageVO.class.getSimpleName(),
                DiskOfferingVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        int op = actionToOpCode(action);

        if (op == OP_DELETE_VOLUME) {
            List<VolumeDeletionStruct> invs = volumesFromAction(action);
            if (invs != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(invs);
            }
        }

        return null;
    }

    private void handlePrimaryStorageDetach(CascadeAction action, Completion completion) {
        List<PrimaryStorageDetachStruct> primaryStorageDetachStruct = action.getParentIssuerContext();
        List<String> primaryStorageUuids = primaryStorageDetachStruct.stream()
                .map(PrimaryStorageDetachStruct::getPrimaryStorageUuid).collect(Collectors.toList());
        List<VolumeVO> volumeVOs = Q.New(VolumeVO.class).in(VolumeVO_.primaryStorageUuid, primaryStorageUuids).list();
        List<DetachDataVolumeFromHostMsg> umsgs =
                volumeVOs.stream().map(volumeVO -> {
                    VolumeHostRefVO refVO = Q.New(VolumeHostRefVO.class).eq(VolumeHostRefVO_.volumeUuid, volumeVO.getUuid()).find();
                    if (refVO == null) {
                        return null;
                    }
                    String volumeInstallPath = Q.New(VolumeVO.class).select(VolumeVO_.installPath)
                            .eq(VolumeVO_.uuid, volumeVO.getUuid()).findValue();
                    return new VolumeHostRef(refVO, volumeInstallPath);
                }).filter(Objects::nonNull).map(volumeHostRef -> {
                    DetachDataVolumeFromHostMsg dmsg = new DetachDataVolumeFromHostMsg();
                    dmsg.setHostUuid(volumeHostRef.refVO.getHostUuid());
                    dmsg.setMountPath(volumeHostRef.refVO.getMountPath());
                    dmsg.setDevice(volumeHostRef.refVO.getDevice());
                    dmsg.setVolumeUuid(volumeHostRef.refVO.getVolumeUuid());
                    dmsg.setVolumeInstallPath(volumeHostRef.volumeInstallPath);
                    bus.makeTargetServiceIdByResourceUuid(dmsg, HostConstant.SERVICE_ID, volumeHostRef.refVO.getHostUuid());
                    return dmsg;
                }).collect(Collectors.toList());

        if (umsgs.isEmpty()) {
            completion.success();
            return;
        }
        new While<>(umsgs).each((umsg, com) -> bus.send(umsg, new CloudBusCallBack(com) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to umount storage path on host, %s", reply.getError()));
                }
                com.done();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }

    static class VolumeHostRef {
        private final VolumeHostRefVO refVO;
        private final String volumeInstallPath;

        VolumeHostRef(VolumeHostRefVO refVO, String volumeInstallPath) {
            this.refVO = refVO;
            this.volumeInstallPath = volumeInstallPath;
        }
    }
}
