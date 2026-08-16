package org.zstack.storage.snapshot;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacadeImpl;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.MulitpleOverlayReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeDeletionPolicyManager.VolumeDeletionPolicy;
import org.zstack.header.volume.VolumeDeletionStruct;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.snapshot.reference.VolumeSnapshotReferenceUtils;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 */
public class VolumeSnapshotCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(VolumeSnapshotCascadeExtension.class);
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ErrorFacadeImpl errf;

    private static final String NAME = VolumeSnapshotVO.class.getSimpleName();

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            handleDeletionCheck(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            handleDeletionCleanup(action, completion);
        } else {
            completion.success();
        }
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        try {
            if (VolumeSnapshotVO.class.getSimpleName().equals(action.getParentIssuer())) {
                VolumeSnapshotDeletionStructs structs = action.getParentIssuerContext();
                List<VolumeSnapshotInventory> sinvs = structs.getSnapshotInventories();
                sinvs.forEach(s -> dbf.eoCleanup(VolumeSnapshotVO.class, s.getUuid()));
            } else {
                dbf.eoCleanup(VolumeSnapshotVO.class);
                dbf.eoCleanup(VolumeSnapshotTreeVO.class);
                dbf.eoCleanup(VolumeSnapshotGroupVO.class);
            }
        } catch (NullPointerException e) {
            logger.warn(e.getLocalizedMessage());
            dbf.eoCleanup(VolumeSnapshotVO.class);
            dbf.eoCleanup(VolumeSnapshotTreeVO.class);
            dbf.eoCleanup(VolumeSnapshotGroupVO.class);
        } finally {
            completion.success();
        }
    }

    private static VolumeSnapshotDeletionMsg buildMsg(final String suuid, String direction, String scope, boolean volumeDeletion) {
        Tuple t = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, suuid)
                .select(VolumeSnapshotVO_.volumeUuid, VolumeSnapshotVO_.treeUuid)
                .findTuple();

        String volumeUuid = t.get(0, String.class);
        String treeUuid = t.get(1, String.class);

        VolumeSnapshotDeletionMsg msg = new VolumeSnapshotDeletionMsg();
        msg.setSnapshotUuid(suuid);
        msg.setTreeUuid(treeUuid);
        msg.setVolumeUuid(volumeUuid);
        msg.setVolumeDeletion(volumeDeletion);
        msg.setDirection(direction);
        msg.setScope(scope);
        return msg;
    }

    private VolumeSnapshotDeletionMsg makeMsg(final String suuid, String direction, String scope, boolean volumeDeletion) {
        VolumeSnapshotDeletionMsg msg = buildMsg(suuid, direction, scope, volumeDeletion);
        setTargetServiceId(msg);
        return msg;
    }

    private void setTargetServiceId(VolumeSnapshotDeletionMsg msg) {
        String resourceUuid = msg.getVolumeUuid() != null ? msg.getVolumeUuid() : msg.getTreeUuid();
        bus.makeTargetServiceIdByResourceUuid(msg, VolumeSnapshotConstant.SERVICE_ID, resourceUuid);
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        final List<VolumeSnapshotDeletionMsg> msgs = new ArrayList<>();
        if (VolumeVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<VolumeDeletionStruct> vols = action.getParentIssuerContext();
            for (VolumeDeletionStruct vol : vols) {
                msgs.addAll(handleVolumeDeletion(vol));
            }
        } else if (VolumeSnapshotVO.class.getSimpleName().equals(action.getParentIssuer())) {
            VolumeSnapshotDeletionStructs struct = action.getParentIssuerContext();
            List<VolumeSnapshotInventory> sinvs = struct.getSnapshotInventories();

            for (VolumeSnapshotInventory sinv : sinvs) {
                msgs.add(handleSnapshotDeletion(sinv, struct.getDirection(), struct.getScope()));
            }
        }

        if (msgs.isEmpty()) {
            completion.success();
            return;
        }

        // To delete volume snapshot, we need first to be synchronized in the volume queue.
        Map<String, List<VolumeSnapshotDeletionMsg>> msgMap = msgs.stream().collect(Collectors.groupingBy(VolumeSnapshotDeletionMsg::getVolumeUuid));
        Map<String, List<VolumeSnapshotInventory>> snapshotToDelete = msgMap.keySet().stream().filter(Objects::nonNull).collect(Collectors.toMap(volUuid -> volUuid, volUuid -> {
            List<String> snapshotUuids = msgMap.get(volUuid).stream().map(VolumeSnapshotDeletionMsg::getSnapshotUuid).collect(Collectors.toList());
            return VolumeSnapshotInventory.valueOf(Q.New(VolumeSnapshotVO.class).in(VolumeSnapshotVO_.uuid, snapshotUuids).list());
        }));

        new While<>(msgMap.entrySet()).step((e, compl) -> {
            VolumeSnapshotOverlayMsg omsg = new VolumeSnapshotOverlayMsg();
            omsg.setMessages(new ArrayList<>(e.getValue()));
            omsg.setVolumeUuid(e.getKey());
            bus.makeTargetServiceIdByResourceUuid(omsg, VolumeConstant.SERVICE_ID, e.getKey());

            bus.send(omsg, new CloudBusCallBack(compl) {
                private List<ErrorCode> buildErrorCodeFromReply(MessageReply r) {
                    List<ErrorCode> errorCodes = new ArrayList<>();
                    Iterator<NeedReplyMessage> iterator = omsg.getMessages().iterator();
                    if (!r.isSuccess()) {
                        omsg.getMessages().forEach(m -> {
                            errorCodes.add(r.getError().copy()
                                    .withOpaque(VolumeSnapshotConstant.SNAPSHOT_UUID, ((VolumeSnapshotDeletionMsg)iterator.next()).getSnapshotUuid()));
                        });
                        return errorCodes;
                    }
                    MulitpleOverlayReply reply = (MulitpleOverlayReply)r;
                    reply.getInnerReplies().forEach(innerReply -> {
                        VolumeSnapshotDeletionMsg innerMsg = (VolumeSnapshotDeletionMsg)iterator.next();
                        if (!innerReply.isSuccess()) {
                            errorCodes.add(innerReply.getError()
                                    .withOpaque(VolumeSnapshotConstant.SNAPSHOT_UUID, innerMsg.getSnapshotUuid()));
                        }
                    });
                    return errorCodes;
                }

                @Override
                public void run(MessageReply reply) {
                    String volumeUuid = omsg.getVolumeUuid();
                    CollectionUtils.safeForEach(pluginRgty.getExtensionList(VolumeSnapshotAfterDeleteExtensionPoint.class), ext -> {
                        ext.volumeSnapshotAfterCleanUpExtensionPoint(volumeUuid, snapshotToDelete.get(volumeUuid));
                    });

                    buildErrorCodeFromReply(reply).forEach(compl::addError);
                    compl.done();
                }
            });
        }, msgMap.entrySet().size()).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE) || errorCodeList.getCauses().isEmpty()) {
                    completion.success();
                    return;
                }
                ErrorCode errorCode = errf.instantiateErrorCode(VolumeSnapshotErrors.BATCH_DELETE_ERROR, "batch delete volume snapshot error", errorCodeList.getCauses());
                completion.fail(errorCode);
            }
        });
    }

    private VolumeSnapshotDeletionMsg handleSnapshotDeletion(VolumeSnapshotInventory sinv, String direction, String scope) {
        return makeMsg(sinv.getUuid(), direction, scope, false);
    }

    private List<VolumeSnapshotDeletionMsg> handleVolumeDeletion(VolumeDeletionStruct vol) {
        if (VolumeDeletionPolicy.Direct.toString().equals(vol.getDeletionPolicy())) {
            List<VolumeSnapshotDeletionMsg> msgs = handleVolumeExpunge(vol.getInventory().getUuid());
            msgs.forEach(this::setTargetServiceId);
            return msgs;
        }

        return Collections.emptyList();
    }

    static List<VolumeSnapshotDeletionMsg> handleVolumeExpunge(String volumeUuid) {
        List<String> cuuids = Q.New(VolumeSnapshotTreeVO.class).select(VolumeSnapshotTreeVO_.uuid)
                .eq(VolumeSnapshotTreeVO_.volumeUuid, volumeUuid)
                .listValues();

        Map<String, List<String>> referenceSnapshotTrees = VolumeSnapshotReferenceUtils
                .getDirectReferencedSnapshotUuidsGroupByTree(volumeUuid);

        return cuuids.stream().flatMap(it ->
                        getDeletableSnapshotUuidOnVolumeExpunge(it, referenceSnapshotTrees.get(it)).stream())
                .map(it -> buildMsg(it, DeleteVolumeSnapshotDirection.Pull.toString(), DeleteVolumeSnapshotScope.Chain.toString(), true))
                .collect(Collectors.toList());
    }

    private static Set<String> getDeletableSnapshotUuidOnVolumeExpunge(String treeUuid, List<String> protectedSnapshotUuids) {
        if (CollectionUtils.isEmpty(protectedSnapshotUuids)) {
            // deleting full snapshot of chain will cause whole chain to be deleted
            String suuid = Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.uuid)
                    .eq(VolumeSnapshotVO_.treeUuid, treeUuid)
                    .isNull(VolumeSnapshotVO_.parentUuid)
                    .findValue();

            if (suuid == null) {
                // this is a storage snapshot, don't delete it on primary storage
                return Collections.emptySet();
            } else {
                return Collections.singleton(suuid);
            }
        }

        List<VolumeSnapshotVO> vos = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.treeUuid, treeUuid).list();
        VolumeSnapshotTree tree = VolumeSnapshotTree.fromVOs(vos);

        Set<String> deletableSnapshotUuids = vos.stream().map(ResourceVO::getUuid).collect(Collectors.toSet());
        deletableSnapshotUuids.removeAll(protectedSnapshotUuids);
        tree.getRoot().walkDownAll(leaf -> {
            if (protectedSnapshotUuids.contains(leaf.getUuid())) {
                deletableSnapshotUuids.retainAll(leaf.getDescendants().stream().map(VolumeSnapshotInventory::getUuid).collect(Collectors.toSet()));
            }
        });

        tree.getRoot().walkDownAll(leaf -> {
            if (leaf.getParent() != null && deletableSnapshotUuids.contains(leaf.getParent().getUuid())) {
                deletableSnapshotUuids.remove(leaf.getUuid());
            }
        });

        return deletableSnapshotUuids;
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(VolumeVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    private List<VolumeSnapshotInventory> fromAction(CascadeAction action) {
        List<VolumeSnapshotInventory> ret = null;
        if (VolumeVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<VolumeDeletionStruct> vols = action.getParentIssuerContext();
            List<String> volUuids = transformAndRemoveNull(vols, arg -> arg.getInventory().getUuid());

            if (volUuids.isEmpty()) {
                return null;
            }

            List<VolumeSnapshotVO> vos = Q.New(VolumeSnapshotVO.class)
                    .in(VolumeSnapshotVO_.volumeUuid, volUuids)
                    .list();
            if (!vos.isEmpty()) {
                ret = VolumeSnapshotInventory.valueOf(vos);
            }
        } else if (NAME.equals(action.getParentIssuer())) {
            ret = action.getParentIssuerContext();
        }

        return ret;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<VolumeSnapshotInventory> invs = fromAction(action);
            if (invs != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(invs);
            }
        }

        return null;
    }
}
