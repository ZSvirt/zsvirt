package org.zstack.storage.snapshot;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.quota.QuotaMessageHandler;
import org.zstack.header.message.*;
import org.zstack.header.storage.backup.CleanUpVmBackupExtensionPoint;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.VolumeSnapshotCapability.VolumeSnapshotArrangementType;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.group.*;
import org.zstack.header.storage.snapshot.reference.VolumeSnapshotReferenceMessage;
import org.zstack.header.vm.*;
import org.zstack.header.vm.devices.*;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.identity.QuotaUtil;
import org.zstack.storage.snapshot.group.MemorySnapshotGroupReferenceFactory;
import org.zstack.storage.snapshot.group.VolumeSnapshotGroupBase;
import org.zstack.storage.snapshot.group.VolumeSnapshotGroupChecker;
import org.zstack.storage.snapshot.reference.VolumeSnapshotReferenceTreeBase;
import org.zstack.storage.snapshot.reference.VolumeSnapshotReferenceUtils;
import org.zstack.storage.volume.FireSnapShotCanonicalEvent;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.tag.TagManager;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zql.ZQL;

import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;

/**
 */
public class VolumeSnapshotManagerImpl extends AbstractService implements
        VolumeSnapshotManager,
        MarshalReplyMessageExtensionPoint,
        VolumeBeforeExpungeExtensionPoint,
        ResourceOwnerAfterChangeExtensionPoint,
        ReportQuotaExtensionPoint,
        AfterReimageVmInstanceExtensionPoint,
        VmJustBeforeDeleteFromDbExtensionPoint,
        VolumeJustBeforeDeleteFromDbExtensionPoint,
        OverwriteVolumeExtensionPoint, FlattenVolumeExtensionPoint,
        CleanUpVmBackupExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VolumeSnapshotManagerImpl.class);
    private String syncSignature;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private CascadeFacade casf;
    @Autowired
    private VmInstanceResourceMetadataManager vidm;

    private Map<String, MemorySnapshotGroupReferenceFactory> referenceFactories = Collections.synchronizedMap(new HashMap<>());


    private void passThrough(VolumeSnapshotMessage msg) {
        VolumeSnapshotVO vo = dbf.findByUuid(msg.getSnapshotUuid(), VolumeSnapshotVO.class);
        if (vo == null) {
            throw new OperationFailureException(err(SysErrors.RESOURCE_NOT_FOUND,
                    "cannot find volume snapshot[uuid:%s]", msg.getSnapshotUuid()
            ));
        }

        if (msg.getVolumeUuid() != null) {
            VolumeSnapshotTreeBase tree = new VolumeSnapshotTreeBase(vo, true);
            tree.handleMessage((Message) msg);
        } else if (msg.getTreeUuid() != null) {
            VolumeSnapshotTreeBase tree = new VolumeSnapshotTreeBase(vo, false);
            tree.handleMessage((Message) msg);
        } else {
            VolumeSnapshot snapshot = new VolumeSnapshotBase(vo);
            snapshot.handleMessage((Message) msg);
        }
    }

    private void passThrough(VolumeSnapshotGroupMessage msg) {
        VolumeSnapshotGroupVO vo = dbf.findByUuid(msg.getGroupUuid(), VolumeSnapshotGroupVO.class);
        if (vo == null) {
            throw new OperationFailureException(err(SysErrors.RESOURCE_NOT_FOUND,
                    "cannot find volume snapshot[uuid:%s]", msg.getGroupUuid()
            ));
        }

        VolumeSnapshotGroupBase group = new VolumeSnapshotGroupBase(vo);
        group.handleMessage((Message) msg);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof VolumeSnapshotMessage) {
            passThrough((VolumeSnapshotMessage) msg);
        } else if (msg instanceof VolumeSnapshotGroupMessage) {
            handleSnapshotGroup((VolumeSnapshotGroupMessage) msg);
        } else if (msg instanceof VolumeSnapshotReferenceMessage) {
            handleSnapshotReference((VolumeSnapshotReferenceMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof CreateVolumeSnapshotMsg) {
            handle((CreateVolumeSnapshotMsg) msg);
        } else if (msg instanceof VolumeSnapshotReportPrimaryStorageCapacityUsageMsg) {
            handle((VolumeSnapshotReportPrimaryStorageCapacityUsageMsg) msg);
        } else if (msg instanceof MarkRootVolumeAsSnapshotMsg) {
            handle((MarkRootVolumeAsSnapshotMsg) msg);
        } else if (msg instanceof AskVolumeSnapshotStructMsg) {
            handle((AskVolumeSnapshotStructMsg) msg);
        } else if (msg instanceof GetVolumeSnapshotTreeRootNodeMsg) {
            handle((GetVolumeSnapshotTreeRootNodeMsg) msg);
        } else if (msg instanceof GetVolumeSnapshotEncryptedMsg) {
            handle((GetVolumeSnapshotEncryptedMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(GetVolumeSnapshotEncryptedMsg msg) {
        GetVolumeSnapshotEncryptedReply snapshotEncryptedReply = new GetVolumeSnapshotEncryptedReply();
        GetVolumeSnapshotEncryptedOnPrimaryStorageMsg encryptedMsg = new GetVolumeSnapshotEncryptedOnPrimaryStorageMsg();
        encryptedMsg.setSnapshotUuid(msg.getSnapshotUuid());
        encryptedMsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        encryptedMsg.setPrimaryStorageInstallPath(msg.getPrimaryStorageInstallPath());
        bus.makeTargetServiceIdByResourceUuid(encryptedMsg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
        bus.send(encryptedMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    snapshotEncryptedReply.setError(reply.getError());
                    bus.reply(msg, snapshotEncryptedReply);
                    return;
                }

                GetVolumeSnapshotEncryptedOnPrimaryStorageReply encryptedReply = reply.castReply();

                snapshotEncryptedReply.setEncrypt(encryptedReply.getEncrypt());
                snapshotEncryptedReply.setSnapshotUuid(encryptedReply.getSnapshotUuid());
                bus.reply(msg, snapshotEncryptedReply);
            }
        });

    }

    private void handleSnapshotGroup(VolumeSnapshotGroupMessage msg) {
        if (msg.getBackendOperation() == SnapshotBackendOperation.NONE || !(msg instanceof APIMessage)) {
            passThrough(msg);
            return;
        }

        List<Tuple> ts = SQL.New("select snap.volumeUuid, snap.primaryStorageUuid" +
                " from VolumeSnapshotVO snap, VolumeSnapshotGroupRefVO ref" +
                " where ref.volumeSnapshotGroupUuid = :groupUuid" +
                " and snap.uuid = ref.volumeSnapshotUuid", Tuple.class)
                .param("groupUuid", msg.getGroupUuid())
                .list();
        List<String> volumeUuids = ts.stream().map(it -> it.get(0, String.class)).filter(Objects::nonNull).collect(Collectors.toList());
        String vmInstanceUuid = volumeUuids.isEmpty() ? null : Q.New(VolumeVO.class).select(VolumeVO_.vmInstanceUuid)
                .in(VolumeVO_.uuid, volumeUuids).eq(VolumeVO_.type, VolumeType.Root).findValue();

        Map<String, List<String>> psVolumeRef = ts.stream().
                filter(t -> t.get(1, String.class) != null)
                .collect(Collectors.groupingBy(t -> ((Tuple) t).get(1, String.class),
                        Collectors.mapping(t -> ((Tuple) t).get(0, String.class), Collectors.toList())));

        final ErrorCode[] err = new ErrorCode[1];
        new While<>(psVolumeRef.entrySet()).each((e, completion) -> {
            CheckVolumeSnapshotOperationOnPrimaryStorageMsg cmsg = new CheckVolumeSnapshotOperationOnPrimaryStorageMsg();
            cmsg.setPrimaryStorageUuid(e.getKey());
            cmsg.setVolumeUuids(e.getValue());
            cmsg.setVmInstanceUuid(vmInstanceUuid);
            cmsg.setOperation(msg.getBackendOperation());
            bus.makeLocalServiceId(cmsg, PrimaryStorageConstant.SERVICE_ID);
            bus.send(cmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        err[0] = reply.getError();
                        completion.allDone();
                        return;
                    }

                    completion.done();
                }
            });
        }).run(new WhileDoneCompletion((Message) msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (err[0] == null) {
                    passThrough(msg);
                    return;
                }

                APIEvent event = new APIEvent(((APIMessage) msg).getId());
                event.setError(err[0]);
                bus.publish(event);
            }
        });
    }

    private void handleSnapshotReference(VolumeSnapshotReferenceMessage msg) {
        VolumeSnapshotReferenceTreeBase tree = new VolumeSnapshotReferenceTreeBase(msg.getTree());
        tree.handleMessage((Message) msg);
    }

    @Transactional(readOnly = true)
    private void handle(VolumeSnapshotReportPrimaryStorageCapacityUsageMsg msg) {
        String sql = "select sum(sp.size)" +
                " from VolumeSnapshotVO sp" +
                " where sp.type = :sptype" +
                " and sp.primaryStorageUuid = :prUuid";
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("sptype", VolumeSnapshotConstant.HYPERVISOR_SNAPSHOT_TYPE.toString());
        q.setParameter("prUuid", msg.getPrimaryStorageUuid());
        Long size = q.getSingleResult();

        VolumeSnapshotReportPrimaryStorageCapacityUsageReply reply = new VolumeSnapshotReportPrimaryStorageCapacityUsageReply();
        reply.setUsedSize(size == null ? 0 : size);
        bus.reply(msg, reply);
    }

    private void handle(APIBatchDeleteVolumeSnapshotMsg msg) {
        APIBatchDeleteVolumeSnapshotEvent event = new APIBatchDeleteVolumeSnapshotEvent(msg.getId());
        Map<String, List<String>> ancestorMap = getAncestorSnapshots(msg.getUuids());

        final String issuer = VolumeSnapshotVO.class.getSimpleName();
        List<VolumeSnapshotVO> vos = Q.New(VolumeSnapshotVO.class).in(VolumeSnapshotVO_.uuid, ancestorMap.keySet()).list();
        VolumeSnapshotDeletionStructs ctx = new VolumeSnapshotDeletionStructs(
                VolumeSnapshotInventory.valueOf(vos), DeleteVolumeSnapshotDirection.Pull.toString(), DeleteVolumeSnapshotScope.Chain.toString());
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("batch-delete-snapshots");
        chain.enableProgressReport();

        if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            }).then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        } else {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        }

        Map<String, BatchDeleteVolumeSnapshotStruct> results = Stream.concat(ancestorMap.keySet().stream(), ancestorMap.values().stream().flatMap(Collection::stream))
                .collect(Collectors.toMap(uuid -> uuid, uuid -> {
                    BatchDeleteVolumeSnapshotStruct r = new BatchDeleteVolumeSnapshotStruct();
                    r.setSnapshotUuid(uuid);
                    return r;
                }));

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                ctx.getSnapshotInventories().stream().filter(inventory -> results.containsKey(inventory.getUuid()) && results.get(inventory.getUuid()).isSuccess())
                        .forEach(inventory -> {
                            new FireSnapShotCanonicalEvent()
                                    .fireSnapShotStatusChangedEvent(VolumeSnapshotStatus.valueOf(inventory.getStatus()), inventory);
                        });
                event.setResults(new ArrayList<>(results.values()));
                bus.publish(event);
            }
        }).error(new FlowErrorHandler(msg) {
            private void handleErrorCode(ErrorCode errorCode) {
                errorCode.getCauses().forEach(err -> {
                    String snapshotUuid = (String) err.getFromOpaque(VolumeSnapshotConstant.SNAPSHOT_UUID);
                    List<String> spUuids = new ArrayList<>();
                    spUuids.add(snapshotUuid);
                    if (CollectionUtils.isNotEmpty(ancestorMap.get(snapshotUuid))) {
                        spUuids.addAll(ancestorMap.get(snapshotUuid));
                    }

                    spUuids.forEach(spUuid -> results.get(spUuid).setError(err));
                });
            }

            @Override
            public void handle(ErrorCode errCode, Map data) {
                if (!errCode.isError(VolumeSnapshotErrors.BATCH_DELETE_ERROR)) {
                    event.setError(errCode);
                    bus.publish(event);
                    return;
                }
                handleErrorCode(errCode);
                event.setResults(new ArrayList<>(results.values()));
                ctx.getSnapshotInventories().stream().filter(inventory -> results.containsKey(inventory.getUuid()) && results.get(inventory.getUuid()).isSuccess())
                        .forEach(inventory -> {
                            new FireSnapShotCanonicalEvent()
                                    .fireSnapShotStatusChangedEvent(VolumeSnapshotStatus.valueOf(inventory.getStatus()), inventory);
                        });
                bus.publish(event);
            }
        }).start();
    }

    @Override
    public void afterCleanUpVmBackup(String groupUuid) {
        vidm.deleteArchiveVmInstanceResourceMetadataGroup(groupUuid);
    }

    private class SnapshotAncestorStruct {
        private List<String> ancestors;
        private boolean child = false;

        public List<String> getAncestors() {
            return ancestors;
        }

        public void setAncestors(List<String> ancestors) {
            this.ancestors = ancestors;
        }

        public boolean isChild() {
            return child;
        }

        public void setChild(boolean child) {
            this.child = child;
        }
    }

    private Map<String, List<String>> getAncestorSnapshots(List<String> snapshotUuids) {
        Map<String, List<String>> result = new HashMap<>();
        Map<String, SnapshotAncestorStruct> snapshotAncestors = new HashMap();
        for (String snapshotUuid : snapshotUuids) {
            SnapshotAncestorStruct snapshotAncestor = new SnapshotAncestorStruct();
            List<String> t = getSnapshotAncestors(snapshotUuid);
            if (t.size() == 1) {
                snapshotAncestor.setAncestors(new ArrayList<>());
            } else {
                t.remove(snapshotUuid);
                snapshotAncestor.setAncestors(t);
            }

            logger.debug(String.format("got ancestors %s of snapshot[uuid: %s]", snapshotAncestor.getAncestors(), snapshotUuid));
            snapshotAncestors.put(snapshotUuid, snapshotAncestor);
        }

        for (String snapshotUuid : snapshotUuids) {
            if (snapshotUuids.parallelStream().anyMatch(s -> snapshotAncestors.get(snapshotUuid).getAncestors().contains(s))) {
                snapshotAncestors.get(snapshotUuid).setChild(true);
            }
        }

        Integer count = 0;
        for (String root: snapshotAncestors.entrySet().parallelStream().filter(e -> !e.getValue().isChild()).map(e -> e.getKey()).collect(Collectors.toList())) {
            List<String> children = snapshotAncestors.entrySet().stream().filter(e -> e.getValue().getAncestors().contains(root)).map(e -> e.getKey()).collect(Collectors.toList());
            logger.debug(String.format("get root snapshot: %s and its children: %s", root, children));
            result.put(root, children);
            count += 1 + children.size();
        }
        DebugUtils.Assert(count == snapshotUuids.size(), String.format("count of snapshotAncestors[%s] not equals to snapshots[%s]", count, snapshotUuids.size()));
        return result;
    }

    private List<String> getSnapshotAncestors(String snapshotUuid) {
        String parentUuid = Q.New(VolumeSnapshotVO.class)
                .select(VolumeSnapshotVO_.parentUuid).eq(VolumeSnapshotVO_.uuid, snapshotUuid).findValue();
        if (parentUuid == null || parentUuid.length() == 0) {
            return Arrays.asList(snapshotUuid);
        }  else {
            List<String> r = new ArrayList<>();
            r.add(snapshotUuid);
            r.addAll(getSnapshotAncestors(parentUuid));
            return r;
        }
    }

    private void handle(GetVolumeSnapshotTreeRootNodeMsg msg) {
        List<Tuple> ts = SQL.New("select snapshot.primaryStorageInstallPath, tree.current from VolumeSnapshotTreeVO tree, VolumeSnapshotVO snapshot" +
                " where tree.volumeUuid = :volUuid" +
                " and snapshot.treeUuid = tree.uuid" +
                " and snapshot.parentUuid is null", Tuple.class)
                .param("volUuid", msg.getVolumeUuid())
                .list();
        GetVolumeSnapshotTreeRootNodeReply reply = new GetVolumeSnapshotTreeRootNodeReply();
        for (Tuple t : ts) {
            if (t.get(1, Boolean.class)) {
                reply.setCurrentRootInstallPath(t.get(0, String.class));
            } else {
                reply.addPreviousRootInstallPath(t.get(0, String.class));
            }
        }
        bus.reply(msg, reply);
    }

    private void handle(APICheckVolumeSnapshotGroupAvailabilityMsg msg) {
        APICheckVolumeSnapshotGroupAvailabilityReply reply = new APICheckVolumeSnapshotGroupAvailabilityReply();
        reply.setResults(VolumeSnapshotGroupChecker.getAvailability(msg.getUuids()));
        bus.reply(msg, reply);
    }

/*
    private void handle(APIGetVolumeSnapshotTreeMsg msg) {
        APIGetVolumeSnapshotTreeReply reply = new APIGetVolumeSnapshotTreeReply();
        if (msg.getTreeUuid() != null) {
            VolumeSnapshotTreeVO treeVO = dbf.findByUuid(msg.getTreeUuid(), VolumeSnapshotTreeVO.class);
            if (treeVO == null) {
                reply.setInventories(new ArrayList<>());
                bus.reply(msg, reply);
                return;
            }

            VolumeSnapshotTreeInventory inv = VolumeSnapshotTreeInventory.valueOf(treeVO);
            List<VolumeSnapshotVO> vos = Q.New(VolumeSnapshotVO.class)
                    .eq(VolumeSnapshotVO_.treeUuid, msg.getTreeUuid())
                    .list();
            VolumeSnapshotTree tree = VolumeSnapshotTree.fromVOs(vos);
            inv.setTree(tree.getRoot().toLeafInventory());
            reply.setInventories(Arrays.asList(inv));
        } else if (msg.getVolumeUuid() != null) {
            SimpleQuery<VolumeSnapshotTreeVO> q = dbf.createQuery(VolumeSnapshotTreeVO.class);
            q.add(VolumeSnapshotTreeVO_.volumeUuid, Op.EQ, msg.getVolumeUuid());
            List<VolumeSnapshotTreeVO> trees = q.list();
            if (trees.isEmpty()) {
                reply.setInventories(new ArrayList<>());
                bus.reply(msg, reply);
                return;
            }

            List<VolumeSnapshotTreeInventory> treeInventories = new ArrayList<>();
            for (VolumeSnapshotTreeVO vo : trees) {
                VolumeSnapshotTreeInventory inv = VolumeSnapshotTreeInventory.valueOf(vo);
                SimpleQuery<VolumeSnapshotVO> sq = dbf.createQuery(VolumeSnapshotVO.class);
                sq.add(VolumeSnapshotVO_.treeUuid, Op.EQ, vo.getUuid());
                List<VolumeSnapshotVO> vos = sq.list();
                VolumeSnapshotTree tree = VolumeSnapshotTree.fromVOs(vos);
                inv.setTree(tree.getRoot().toLeafInventory());
                treeInventories.add(inv);
            }

            reply.setInventories(treeInventories);
        }

        bus.reply(msg, reply);
    }
*/

    private VolumeSnapshotStruct newChain(VolumeSnapshotVO vo, boolean fullsnapshot) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                VolumeSnapshotTreeVO chain = new VolumeSnapshotTreeVO();
                chain.setCurrent(false);
                chain.setVolumeUuid(vo.getVolumeUuid());
                chain.setUuid(Platform.getUuid());
                chain.setStatus(VolumeSnapshotTreeStatus.Creating);
                if (!fullsnapshot) {
                    String rootImageUuid = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, vo.getVolumeUuid())
                            .select(VolumeVO_.rootImageUuid)
                            .findValue();
                    chain.setRootImageUuid(rootImageUuid);
                }

                chain = dbf.getEntityManager().merge(chain);

                logger.debug(String.format("created new volume snapshot tree[tree uuid:%s, volume uuid:%s, full snapshot uuid:%s]",
                        chain.getUuid(), vo.getVolumeUuid(), vo.getUuid()));

                vo.setTreeUuid(chain.getUuid());
                vo.setDistance(fullsnapshot ? 0 : 1);
                vo.setParentUuid(null);
                vo.setLatest(true);
                vo.setFullSnapshot(fullsnapshot);
                dbf.getEntityManager().persist(vo);
                dbf.getEntityManager().flush();
                dbf.getEntityManager().refresh(vo);
            }
        }.execute();


        VolumeSnapshotStruct struct = new VolumeSnapshotStruct();
        struct.setCurrent(VolumeSnapshotInventory.valueOf(vo));
        struct.setFullSnapshot(fullsnapshot);
        struct.setNewChain(true);

        return struct;
    }

    public static VolumeSnapshotTreeInventory getCurrentTree(String volumeUuid) {
        VolumeSnapshotTreeVO vo = Q.New(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.volumeUuid, volumeUuid).eq(VolumeSnapshotTreeVO_.current, true).find();
        if (vo != null) {
            return VolumeSnapshotTreeInventory.valueOf(vo);
        } else {
            return null;
        }
    }

    public static void markSnapshotTreeCompleted(VolumeSnapshotInventory snapshot) {
        SQL.New("update VolumeSnapshotTreeVO tree" +
                " set tree.current = false" +
                " where tree.current = true" +
                " and tree.volumeUuid = :volUuid").param("volUuid", snapshot.getVolumeUuid()).execute();

        VolumeSnapshotTreeVO chain = Q.New(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.uuid, snapshot.getTreeUuid()).find();
        DebugUtils.Assert(chain != null, "why not found volumeSnapshotTree here?");

        SQL.New("update VolumeSnapshotTreeVO tree" +
                " set tree.current = true, tree.status = :status" +
                " where tree.uuid = :uuid").param("uuid", snapshot.getTreeUuid()).
                param("status", VolumeSnapshotTreeStatus.Completed).execute();

        logger.debug(String.format("mark new volume snapshot tree[tree uuid:%s, snapshot: %s] to Completed",
                chain.getUuid(), snapshot.getUuid()));
    }

    private VolumeSnapshotStruct saveChainTypeSnapshot(VolumeSnapshotVO vo) {
        return saveChainTypeSnapshot(vo, SnapshotMode.AUTO);
    }

    @Transactional
    private VolumeSnapshotStruct saveChainTypeSnapshot(VolumeSnapshotVO vo, SnapshotMode mode) {
        String sql = "select c" +
                " from VolumeSnapshotTreeVO c" +
                " where c.volumeUuid = :volUuid" +
                " and c.current = true";
        TypedQuery<VolumeSnapshotTreeVO> cq = dbf.getEntityManager().createQuery(sql, VolumeSnapshotTreeVO.class);
        cq.setParameter("volUuid", vo.getVolumeUuid());
        List<VolumeSnapshotTreeVO> rets = cq.getResultList();
        DebugUtils.Assert(rets.size() < 2, "can not have more than one VolumeSnapshotTreeVO with current=1");
        VolumeSnapshotTreeVO chain = rets.isEmpty() ? null : rets.get(0);
        final Integer maxIncrementalSnapshotNum = getMaxIncrementalSnapshotNum(vo.getVolumeUuid());
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            if (maxIncrementalSnapshotNum <= 1) {
                throw new OperationFailureException(operr("Unsupported maximum snapshot number (%d) for volume [uuid:%s]",
                        maxIncrementalSnapshotNum, vo.getVolumeUuid()
                ));
            }
        }

        if (chain == null) {
            return newChain(vo, SnapshotMode.isFullSnapShot(mode, () -> maxIncrementalSnapshotNum == 0));
        } else {
            sql = "select s" +
                    " from VolumeSnapshotVO s" +
                    " where s.latest = true" +
                    " and s.volumeUuid = :volUuid" +
                    " and s.treeUuid = :chainUuid";
            TypedQuery<VolumeSnapshotVO> q = dbf.getEntityManager().createQuery(sql, VolumeSnapshotVO.class);
            q.setParameter("volUuid", vo.getVolumeUuid());
            q.setParameter("chainUuid", chain.getUuid());
            VolumeSnapshotVO latest = q.getSingleResult();

            VolumeSnapshotVO finalLatest = latest;
            if (SnapshotMode.isFullSnapShot(mode, () -> finalLatest.getDistance() >= maxIncrementalSnapshotNum)) {
                dbf.getEntityManager().merge(chain);
                return newChain(vo, true);
            }

            latest.setLatest(false);
            latest = dbf.getEntityManager().merge(latest);

            vo.setTreeUuid(latest.getTreeUuid());
            vo.setLatest(true);
            vo.setParentUuid(latest.getUuid());
            vo.setDistance(latest.getDistance() + 1);
            dbf.getEntityManager().persist(vo);
            dbf.getEntityManager().flush();
            dbf.getEntityManager().refresh(vo);

            VolumeSnapshotStruct struct = new VolumeSnapshotStruct();
            struct.setParent(VolumeSnapshotInventory.valueOf(latest));
            struct.setCurrent(VolumeSnapshotInventory.valueOf(vo));
            return struct;
        }
    }

    private Integer getMaxIncrementalSnapshotNum(String volumeUuid) {
        String systemTagValue = VolumeSystemTags.VOLUME_MAX_INCREMENTAL_SNAPSHOT_NUM.getTokenByResourceUuid(volumeUuid,
                VolumeSystemTags.VOLUME_MAX_INCREMENTAL_SNAPSHOT_NUM_TOKEN);
        return systemTagValue != null ? Integer.valueOf(systemTagValue) : VolumeSnapshotGlobalConfig.MAX_INCREMENTAL_SNAPSHOT_NUM.value(Integer.class);
    }

    @Transactional
    private VolumeSnapshotStruct saveIndividualTypeSnapshot(VolumeSnapshotVO vo) {
        return newChain(vo, false);
    }

    @Transactional
    private void rollbackSnapshot(String uuid) {
        VolumeSnapshotVO vo = dbf.getEntityManager().find(VolumeSnapshotVO.class, uuid);
        if (vo == null) {
            return;
        }

        dbf.getEntityManager().remove(vo);

        SQL.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceUuid, uuid)
                .eq(AccountResourceRefVO_.resourceType, VolumeSnapshotVO.class.getSimpleName())
                .delete();

        if (vo.getParentUuid() != null) {
            VolumeSnapshotVO parent = dbf.getEntityManager().find(VolumeSnapshotVO.class, vo.getParentUuid());
            parent.setLatest(true);
            dbf.getEntityManager().merge(parent);
        } else {
            VolumeSnapshotTreeVO chain = dbf.getEntityManager().find(VolumeSnapshotTreeVO.class, vo.getTreeUuid());
            dbf.getEntityManager().remove(chain);
        }
    }

    private void handle(final AskVolumeSnapshotStructMsg msg) {
        AskVolumeSnapshotStructReply reply = new AskVolumeSnapshotStructReply();
        CreateVolumeSnapshotMsg cmsg = new CreateVolumeSnapshotMsg();
        cmsg.setResourceUuid(msg.getResourceUuid());
        cmsg.setVolumeUuid(msg.getVolumeUuid());
        cmsg.setDescription(msg.getDescription());
        cmsg.setName(msg.getName());
        cmsg.setAccountUuid(msg.getAccountUuid());

        reply.setStruct(getVolumeSnapshotStruct(cmsg));
        bus.reply(msg, reply);
    }

    private VolumeSnapshotStruct getVolumeSnapshotStruct(CreateVolumeSnapshotMsg msg) {
        final VolumeVO vol = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        final String primaryStorageUuid = vol.getPrimaryStorageUuid();

        AskVolumeSnapshotCapabilityMsg askMsg = new AskVolumeSnapshotCapabilityMsg();
        askMsg.setPrimaryStorageUuid(primaryStorageUuid);
        askMsg.setVolume(VolumeInventory.valueOf(vol));
        bus.makeLocalServiceId(askMsg, PrimaryStorageConstant.SERVICE_ID);
        MessageReply reply = bus.call(askMsg);
        if (!reply.isSuccess()) {
            throw new OperationFailureException(operr("cannot ask primary storage[uuid:%s] for volume snapshot capability, see detail [%s]", vol.getUuid(),reply.getError()));
        }

        AskVolumeSnapshotCapabilityReply areply = reply.castReply();
        VolumeSnapshotCapability capability = areply.getCapability();
        if (!capability.isSupport()) {
            throw new OperationFailureException(operr("primary storage[uuid:%s] doesn't support volume snapshot;" +
                    " cannot create snapshot for volume[uuid:%s]", primaryStorageUuid, vol.getUuid()));
        }

        final VolumeSnapshotVO vo = new VolumeSnapshotVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setVolumeUuid(msg.getVolumeUuid());
        vo.setFormat(vol.getFormat());
        vo.setEncrypted(vol.isEncrypted());
        vo.setState(VolumeSnapshotState.Enabled);
        vo.setStatus(VolumeSnapshotStatus.Creating);
        vo.setVolumeType(vol.getType().toString());
        vo.setAccountUuid(msg.getAccountUuid());

        final VolumeSnapshotStruct struct = new SQLBatchWithReturn<VolumeSnapshotStruct>() {
            @Override
            protected VolumeSnapshotStruct scripts() {
                VolumeSnapshotStruct s = null;
                if (VolumeSnapshotArrangementType.CHAIN == capability.getArrangementType()) {
                    s = saveChainTypeSnapshot(vo, msg.getRequiredSnapshotMode());
                } else if (VolumeSnapshotArrangementType.INDIVIDUAL == capability.getArrangementType()) {
                    s = saveIndividualTypeSnapshot(vo);
                } else {
                    DebugUtils.Assert(false, "should not be here");
                }

                return s;
            }
        }.execute();

        return struct;
    }

    private void handle(final CreateVolumeSnapshotMsg msg) {
        final CreateVolumeSnapshotReply ret = new CreateVolumeSnapshotReply();
        final VolumeVO vol = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        final String primaryStorageUuid = vol.getPrimaryStorageUuid();


        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("take-volume-snapshot-for-volume-%s", msg.getVolumeUuid()));
        chain.then(new ShareFlow() {
            VolumeSnapshotInventory snapshot;
            String volumeNewInstallPath;
            VolumeSnapshotStruct struct;
            long volumeSize;
            String allocatedInstall;
            long allocatedSize;

            VolumeVO volumeVO = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "create-new-tree-if-needed";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        struct = getVolumeSnapshotStruct(msg);
                        trigger.next();
                    }
                });

                if (volumeVO.getType().equals(VolumeType.Memory)) {
                    volumeSize = Q.New(VmInstanceVO.class)
                            .select(VmInstanceVO_.memorySize)
                            .eq(VmInstanceVO_.uuid, volumeVO.getVmInstanceUuid())
                            .findValue();
                } else {
                    flow(new NoRollbackFlow() {
                        String __name__ = "sync-root-volume-size";
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            SyncVolumeSizeOnPrimaryStorageMsg smsg = new SyncVolumeSizeOnPrimaryStorageMsg();

                            smsg.setPrimaryStorageUuid(volumeVO.getPrimaryStorageUuid());
                            smsg.setVolumeUuid(volumeVO.getUuid());
                            smsg.setInstallPath(volumeVO.getInstallPath());
                            bus.makeTargetServiceIdByResourceUuid(smsg, PrimaryStorageConstant.SERVICE_ID, volumeVO.getPrimaryStorageUuid());
                            bus.send(smsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        trigger.fail(reply.getError());
                                        return;
                                    }

                                    SyncVolumeSizeOnPrimaryStorageReply r = reply.castReply();
                                    volumeSize = r.getActualSize();

                                    trigger.next();
                                }

                            });
                        }
                    });
                }

                flow(new Flow() {
                    String __name__ = "pre-allocate-primary-storage-capacity";

                    @Override
                    public boolean skip(Map data) {
                        return volumeSize <= 0;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
                        amsg.setRequiredPrimaryStorageUuid(vol.getPrimaryStorageUuid());
                        amsg.setSize(volumeSize);
                        amsg.setRequiredInstallUri(String.format("volume://%s", msg.getVolumeUuid()));
                        amsg.setNoOverProvisioning(true);

                        bus.makeTargetServiceIdByResourceUuid(amsg, PrimaryStorageConstant.SERVICE_ID, vol.getPrimaryStorageUuid());
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }
                                allocatedSize = volumeSize;
                                allocatedInstall = ((AllocatePrimaryStorageSpaceReply) reply).getAllocatedInstallUrl();
                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        // rollback when no snapshot created
                        if (snapshot == null && allocatedInstall != null) {
                            ReleasePrimaryStorageSpaceMsg rmsg = new ReleasePrimaryStorageSpaceMsg();
                            rmsg.setPrimaryStorageUuid(vol.getPrimaryStorageUuid());
                            rmsg.setDiskSize(allocatedSize);
                            rmsg.setNoOverProvisioning(true);
                            rmsg.setAllocatedInstallUrl(allocatedInstall);
                            bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, vol.getPrimaryStorageUuid());
                            bus.send(rmsg);
                        }
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "take-volume-snapshot";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        final TakeSnapshotMsg tmsg = new TakeSnapshotMsg();
                        tmsg.setPrimaryStorageUuid(primaryStorageUuid);
                        tmsg.setStruct(struct);
                        tmsg.setName(msg.getName());
                        tmsg.setDescription(msg.getDescription());
                        bus.makeTargetServiceIdByResourceUuid(tmsg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
                        bus.send(tmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                TakeSnapshotReply treply = (TakeSnapshotReply) reply;
                                volumeNewInstallPath = treply.getNewVolumeInstallPath();
                                snapshot = treply.getInventory();
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "after-volume-snapshot-created";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<VolumeSnapshotCreationExtensionPoint> extensionList = pluginRgty.getExtensionList(VolumeSnapshotCreationExtensionPoint.class);
                        new While<>(extensionList).each((ext, wc) -> {
                            ext.afterVolumeSnapshotCreated(snapshot, new Completion(trigger) {
                                @Override
                                public void success() {
                                    wc.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    wc.addError(errorCode);
                                    wc.allDone();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errorCodeList.getCauses().isEmpty()) {
                                    trigger.fail(errorCodeList.getCauses().get(0));
                                    return;
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "adjust-snapshot-capacity-on-primary-storage";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (snapshot.getSize() == volumeSize) {
                            trigger.next();
                            return;
                        }

                        if (snapshot.getSize() - volumeSize > 0) {
                            logger.debug(String.format("reserving primary storage capacity: " +
                                            "snapshot[uuid:%s, size:%s] is larger than Volume[uuid:%s, size:%s]",
                                    snapshot.getUuid(), snapshot.getSize(), vol.getUuid(), volumeSize));
                            AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
                            amsg.setRequiredPrimaryStorageUuid(vol.getPrimaryStorageUuid());
                            amsg.setRequiredInstallUri(String.format("volume://%s", snapshot.getVolumeUuid()));
                            amsg.setSize(snapshot.getSize() - volumeSize);
                            amsg.setForce(true);
                            amsg.setNoOverProvisioning(true);
                            bus.makeTargetServiceIdByResourceUuid(amsg, PrimaryStorageConstant.SERVICE_ID, vol.getPrimaryStorageUuid());
                            bus.send(amsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        trigger.next();
                                        return;
                                    }
                                    allocatedSize = snapshot.getSize();
                                    trigger.next();
                                }
                            });
                            return;
                        }

                        if (volumeSize - snapshot.getSize() > 0) {
                            logger.debug(String.format("releasing primary storage capacity: " +
                                            "snapshot[uuid:%s, size:%s] is smaller than Volume[uuid:%s, size:%s]",
                                    snapshot.getUuid(), snapshot.getSize(), vol.getUuid(), volumeSize));
                            ReleasePrimaryStorageSpaceMsg rmsg = new ReleasePrimaryStorageSpaceMsg();
                            rmsg.setPrimaryStorageUuid(vol.getPrimaryStorageUuid());
                            rmsg.setDiskSize(volumeSize - snapshot.getSize());
                            rmsg.setNoOverProvisioning(true);
                            rmsg.setAllocatedInstallUrl(allocatedInstall);
                            bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, vol.getPrimaryStorageUuid());
                            bus.send(rmsg);
                            allocatedSize = snapshot.getSize();
                            trigger.next();
                        }
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        markSnapshotTreeCompleted(snapshot);
                        if (volumeNewInstallPath != null) {
                            vol.setInstallPath(volumeNewInstallPath);
                            if (Boolean.TRUE.equals(snapshot.getEncrypted())) {
                                vol.setEncrypted(true);
                            }
                            dbf.update(vol);
                        }

                        VolumeSnapshotVO svo = dbf.findByUuid(snapshot.getUuid(), VolumeSnapshotVO.class);
                        svo.setType(snapshot.getType());
                        svo.setPrimaryStorageUuid(snapshot.getPrimaryStorageUuid());
                        svo.setPrimaryStorageInstallPath(snapshot.getPrimaryStorageInstallPath());
                        svo.setStatus(VolumeSnapshotStatus.Ready);
                        svo.setSize(snapshot.getSize());
                        svo.setEncrypted(Boolean.TRUE.equals(snapshot.getEncrypted()));
                        if (snapshot.getFormat() != null) {
                            svo.setFormat(snapshot.getFormat());
                        }
                        svo = dbf.updateAndRefresh(svo);

                        if (struct.isNewChain()) {
                            VolumeSnapshotReferenceUtils.updateReferenceAfterFirstSnapshot(svo);
                        }

                        new FireSnapShotCanonicalEvent().
                                fireSnapShotStatusChangedEvent(
                                        VolumeSnapshotStatus.valueOf(snapshot.getStatus()),
                                        VolumeSnapshotInventory.valueOf(svo));

                        VolumeSnapshotInventory sp = svo.toInventory();
                        callExtensionPoints(sp);

                        ret.setInventory(sp);
                        bus.reply(msg, ret);
                    }

                    @ExceptionSafe
                    private void callExtensionPoints(VolumeSnapshotInventory sp) {
                        VolumeInventory volinv = vol.toInventory();
                        pluginRgty.getExtensionList(VolumeSnapshotAfterCreateExtensionPoint.class).forEach(ext -> ext.volumeSnapshotAfterCreate(volinv, sp));
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        if (struct != null) {
                            rollbackSnapshot(struct.getCurrent().getUuid());
                        }
                        ret.setError(errCode);
                        bus.reply(msg, ret);
                    }
                });
            }
        }).start();
    }

    private void handle(MarkRootVolumeAsSnapshotMsg msg) {
        final MarkRootVolumeAsSnapshotReply ret = new MarkRootVolumeAsSnapshotReply();
        VolumeInventory vol = msg.getVolume();

        String psType = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.type).eq(PrimaryStorageVO_.uuid, vol.getPrimaryStorageUuid()).findValue();
        if (psType == null) {
            ret.setError(operr("cannot find type for primaryStorage [%s]", vol.getPrimaryStorageUuid()));
            bus.reply(msg, ret);
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("mark-rootVolume-%s-as-snapshot", vol.getUuid()));
        chain.then(new ShareFlow() {
            VolumeSnapshotVO vo = new VolumeSnapshotVO();
            VolumeSnapshotCapability capability;
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "ask-volume-snapshot-capability";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        final String primaryStorageUuid = msg.getVolume().getPrimaryStorageUuid();
                        AskVolumeSnapshotCapabilityMsg askMsg = new AskVolumeSnapshotCapabilityMsg();
                        askMsg.setPrimaryStorageUuid(primaryStorageUuid);
                        askMsg.setVolume(vol);
                        bus.makeTargetServiceIdByResourceUuid(askMsg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
                        MessageReply reply = bus.call(askMsg);
                        if (!reply.isSuccess()) {
                            ret.setError(operr("cannot ask primary storage[uuid:%s] for volume snapshot capability",
                                    vol.getUuid()).withCause(reply.getError()));
                            bus.reply(msg, ret);
                            trigger.fail(ret.getError());
                            return;
                        }

                        AskVolumeSnapshotCapabilityReply areply = reply.castReply();
                        capability = areply.getCapability();
                        if (!capability.isSupport()) {
                            ret.setError(operr("primary storage[uuid:%s] doesn't support volume snapshot;" +
                                    " cannot create snapshot for volume[uuid:%s]", primaryStorageUuid, vol.getUuid()));
                            bus.reply(msg, ret);
                            trigger.fail(ret.getError());
                            return;
                        }
                        trigger.next();
                    }
                });

                // mark volume installPath as snapshot installPath, or other extentions...
                List<MarkRootVolumeAsSnapshotExtension> extensions = pluginRgty.getExtensionList(MarkRootVolumeAsSnapshotExtension.class);
                List<Flow> customizedFlows = null;
                for(MarkRootVolumeAsSnapshotExtension extension : extensions){
                    if (psType.equals(extension.getExtensionPrimaryStorageType())) {
                        customizedFlows = extension.markRootVolumeAsSnapshot(vol, vo, msg.getAccountUuid());
                        break;
                    }
                }

                if (customizedFlows != null) {
                    customizedFlows.forEach(this::flow);
                } else {
                    flow(new NoRollbackFlow() {
                        String __name__ = "mark-rootVolume-as-snapshot";
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            vo.setUuid(Platform.getUuid());
                            vo.setName(vol.getName());
                            vo.setDescription(vol.getDescription());
                            vo.setVolumeUuid(vol.getUuid());
                            vo.setFormat(vol.getFormat());
                            vo.setVolumeType(vol.getType());
                            if (vo.getType() == null) {
                                vo.setType(VolumeSnapshotConstant.HYPERVISOR_SNAPSHOT_TYPE.toString());
                            }
                            vo.setPrimaryStorageUuid(vol.getPrimaryStorageUuid());
                            vo.setSize(vol.getSize());
                            vo.setState(VolumeSnapshotState.Enabled);
                            vo.setStatus(VolumeSnapshotStatus.Creating);
                            vo.setAccountUuid(msg.getAccountUuid());

                            if (VolumeSnapshotArrangementType.CHAIN == capability.getArrangementType()) {
                                saveChainTypeSnapshot(vo);
                            } else if (VolumeSnapshotArrangementType.INDIVIDUAL == capability.getArrangementType()) {
                                saveIndividualTypeSnapshot(vo);
                            } else {
                                DebugUtils.Assert(false, "should not be here");
                            }

                            trigger.next();
                        }
                    });
                }

                flow(new NoRollbackFlow() {
                    String __name__ = "post-mark-rootVolume-as-snapshot";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        String uuid = vo.getUuid();
                        vo = dbf.findByUuid(uuid, VolumeSnapshotVO.class);
                        if (vo == null) {
                            trigger.fail(operr("cannot find snapshot: %s", uuid));
                            return;
                        }
                        List<PostMarkRootVolumeAsSnapshotExtension> extensions = pluginRgty.getExtensionList(PostMarkRootVolumeAsSnapshotExtension.class);
                        for(PostMarkRootVolumeAsSnapshotExtension extension : extensions){
                            extension.afterMarkRootVolumeAsSnapshot(VolumeSnapshotInventory.valueOf(vo));
                        }
                        trigger.next();
                    }
                });


                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        markSnapshotTreeCompleted(VolumeSnapshotInventory.valueOf(vo));
                        VolumeSnapshotVO svo = dbf.findByUuid(vo.getUuid(), VolumeSnapshotVO.class);
                        if (svo.getPrimaryStorageInstallPath() == null) {
                            svo.setPrimaryStorageInstallPath(vol.getInstallPath());
                        }
                        svo.setStatus(VolumeSnapshotStatus.Ready);
                        if (vol.getFormat() != null) {
                            svo.setFormat(vol.getFormat());
                        }
                        svo = dbf.updateAndRefresh(svo);
                        tagMgr.createNonInherentSystemTag(svo.getUuid(),
                                VolumeSnapshotSystemTags.VOLUMESNAPSHOT_CREATED_BY_SYSTEM.getTagFormat(),
                                VolumeSnapshotVO.class.getSimpleName());
                        new FireSnapShotCanonicalEvent().
                                fireSnapShotStatusChangedEvent(svo.getStatus(), VolumeSnapshotInventory.valueOf(svo));
                        ret.setInventory(VolumeSnapshotInventory.valueOf(svo));
                        bus.reply(msg, ret);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        rollbackSnapshot(vo.getUuid());
                        ret.setError(errCode);
                        bus.reply(msg, ret);
                    }
                });
            }
        }).start();
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIBatchDeleteVolumeSnapshotMsg) {
            handle((APIBatchDeleteVolumeSnapshotMsg) msg);
        } else if (msg instanceof APICheckVolumeSnapshotGroupAvailabilityMsg) {
            handle((APICheckVolumeSnapshotGroupAvailabilityMsg) msg);
        } else if (msg instanceof APIGetMemorySnapshotGroupReferenceMsg) {
            handle((APIGetMemorySnapshotGroupReferenceMsg) msg);
        } else if (msg instanceof APICheckMemorySnapshotGroupConflictMsg) {
            handle((APICheckMemorySnapshotGroupConflictMsg) msg);
        } else  {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetMemorySnapshotGroupReferenceMsg msg) {
        APIGetMemorySnapshotGroupReferenceReply reply = new APIGetMemorySnapshotGroupReferenceReply(msg.getResourceUuid());

        MemorySnapshotGroupReferenceFactory factory = referenceFactories.get(msg.getResourceType());
        if (factory != null) {
            reply.setInventories(factory.getVolumeSnapshotGroupReferenceList(msg.getResourceUuid()));
            bus.reply(msg, reply);
            return;
        }

        reply.setError(operr("this resource type %s does not support querying memory snapshot references", msg.getResourceType()));
        bus.reply(msg, reply);
    }

    private void handle(APICheckMemorySnapshotGroupConflictMsg msg) {
        APICheckMemorySnapshotGroupConflictReply reply = new APICheckMemorySnapshotGroupConflictReply();

        VmInstanceResourceMetadataGroupVO metadataGroupVO = Q.New(VmInstanceResourceMetadataGroupVO.class)
                .eq(VmInstanceResourceMetadataGroupVO_.resourceUuid, msg.getUuid()).find();
        if (metadataGroupVO == null) {
            throw new ApiMessageInterceptionException(
                    operr("cannot find VmInstanceResourceMetadataGroupVO of the memory snapshot group[uuid:%s]", msg.getUuid()));
        }
        // Retrieve ArchiveVmNicBundle archives related to memory snapshot groups
        List<VmNicInventory> vmNics = metadataGroupVO.getAddressList().stream()
                .filter(vo -> Objects.equals(vo.getMetadataClass(), ArchiveVmNicBundle.class.getCanonicalName()))
                .map(vo -> JSONObjectUtil.toObject(vo.getMetadata(), ArchiveVmNicBundle.class).getVmNicInventory())
                .filter(Objects::nonNull).collect(Collectors.toList());

        // Collect IP and MAC from memory snapshot vmNic
        Set<String> groupIps = vmNics.stream().map(VmNicInventory::getIp).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> groupMacs = vmNics.stream().map(VmNicInventory::getMac).filter(Objects::nonNull).collect(Collectors.toSet());

        String groupVmUuid = Q.New(VolumeSnapshotGroupVO.class).eq(VolumeSnapshotGroupVO_.uuid, msg.getUuid()).select(VolumeSnapshotGroupVO_.vmInstanceUuid).findValue();

        List<VmNicConflictEntry> vmNetworkConflictEntries = new ArrayList<>();
        Set<String> conflictVmUuids = new HashSet<>();

        long count = Q.New(VmNicVO.class).notEq(VmNicVO_.vmInstanceUuid, groupVmUuid).count();
        String sql = String.format("select vmNicVO from VmNicVO vmNicVO where vmNicVO.vmInstanceUuid != '%s'", groupVmUuid);
        SQL.New(sql, VmNicVO.class).limit(100).paginate(count, (List<VmNicVO> vmNicVOs) -> vmNicVOs.forEach(vmNicVO -> {
            String vmUuid = vmNicVO.getVmInstanceUuid();
            String ip = vmNicVO.getIp();
            String mac = vmNicVO.getMac();

            boolean isConflict = false;
            VmNicConflictEntry vmNicConflictEntry = new VmNicConflictEntry();
            if (ip != null && groupIps.contains(ip)) {
                vmNicConflictEntry.setIp(ip);
                isConflict = true;
            }
            if (mac != null && groupMacs.contains(mac)) {
                vmNicConflictEntry.setMac(mac);
                isConflict = true;
            }

            if (!isConflict) {
                return;
            }
            vmNicConflictEntry.setVmNicName(vmNicVO.getInternalName());
            vmNicConflictEntry.setVmInstanceUuid(vmUuid);
            conflictVmUuids.add(vmUuid);
            vmNetworkConflictEntries.add(vmNicConflictEntry);
        }));

        if (!conflictVmUuids.isEmpty()) {
            List<Tuple> conflictVmNames = Q.New(VmInstanceVO.class).in(VmInstanceVO_.uuid, conflictVmUuids).select(VmInstanceVO_.uuid, VmInstanceVO_.name).listTuple();
            Map<String, String> vmNameByVmUuid = conflictVmNames.stream().collect(Collectors.toMap(t -> t.get(0, String.class), t -> t.get(1, String.class)));
            vmNetworkConflictEntries.forEach(vmNicConflictEntry -> vmNicConflictEntry.setVmInstanceName(vmNameByVmUuid.get(vmNicConflictEntry.getVmInstanceUuid())));
        }

        reply.setVmNicConflict(vmNetworkConflictEntries);
        bus.reply(msg, reply);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VolumeSnapshotConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        for (MemorySnapshotGroupReferenceFactory f : pluginRgty.getExtensionList(MemorySnapshotGroupReferenceFactory.class)) {
            MemorySnapshotGroupReferenceFactory old = referenceFactories.get(f.getReferenceResourceType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate MemorySnapshotGroupReferenceFactory[%s, %s] for type[%s]",
                        old.getClass().getName(), f.getClass().getName(), f.getReferenceResourceType()));
            }
            referenceFactories.put(f.getReferenceResourceType(), f);
        }

        pluginRgty.saveExtensionAsMap(CreateTemplateFromVolumeSnapshotExtensionPoint.class,
                (Function<Object, CreateTemplateFromVolumeSnapshotExtensionPoint>) arg -> arg.createTemplateFromVolumeSnapshotPrimaryStorageType());

        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            public void beforeDeliveryMessage(Message msg) {
                if (msg instanceof NeedQuotaCheckMessage) {
                    if (((NeedQuotaCheckMessage) msg).getAccountUuid() == null ||
                            ((NeedQuotaCheckMessage) msg).getAccountUuid().equals("")) {
                        // skip admin scheduler
                        return;
                    }
                    List<Quota> quotas = acntMgr.getMessageQuotaMap().get(msg.getClass());
                    if (quotas == null || quotas.size() == 0) {
                        return;
                    }
                    Map<String, Quota.QuotaPair> pairs = new QuotaUtil().
                            makeQuotaPairs(((NeedQuotaCheckMessage) msg).getAccountUuid());
                    for (Quota quota : quotas) {
                        quota.getOperator().checkQuota((NeedQuotaCheckMessage) msg, pairs);
                    }
                }
            }
        }, VolumeCreateSnapshotMsg.class, CreateVolumeSnapshotMsg.class);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public List<Class> getReplyMessageClassForMarshalExtensionPoint() {
        List<Class> ret = new ArrayList<>();
        ret.add(APIQueryVolumeSnapshotTreeReply.class);
        return ret;
    }

    @Override
    public void marshalReplyMessageBeforeSending(Message replyOrEvent, NeedReplyMessage msg) {
        if (replyOrEvent instanceof APIQueryVolumeSnapshotTreeReply) {
            marshal(((APIMessage) msg).getSession(), (APIQueryVolumeSnapshotTreeReply) replyOrEvent);
        }
    }

    private void marshal(SessionInventory session, APIQueryVolumeSnapshotTreeReply reply) {
        if (reply.getInventories() == null) {
            // this is for count
            return;
        }

        for (VolumeSnapshotTreeInventory inv : reply.getInventories()) {
            List<VolumeSnapshotVO> vos = Q.New(VolumeSnapshotVO.class)
                    .eq(VolumeSnapshotVO_.treeUuid, inv.getUuid())
                    .list();
            VolumeSnapshotTree tree = VolumeSnapshotTree.fromVOs(vos);
            inv.setTree(tree.getRoot().toLeafInventory(querySnapshotUuids(inv.getUuid(), session)));
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> querySnapshotUuids(String treeUuid, SessionInventory session) {
        String zql = String.format("query volumesnapshot.uuid where treeUuid = '%s'", treeUuid);
        List<Object> invs = ZQL.fromString(zql).getSingleResultWithSession(session).inventories;
        return invs != null ? invs.stream().map(it -> ((VolumeSnapshotInventory) it).getUuid()).collect(Collectors.toSet())
                : Collections.emptySet();
    }

    @Override
    public void volumePreExpunge(VolumeInventory volume) {}

    @Override
    public boolean skipExpungeVolume(VolumeInventory volume) {
        return VolumeSnapshotReferenceUtils.isVolumeDirectlyReferenceByOthers(volume);
    }

    @Override
    public void volumeBeforeExpunge(VolumeInventory volume, Completion completion) {
        List<VolumeSnapshotDeletionMsg> msgs = VolumeSnapshotCascadeExtension.handleVolumeExpunge(volume.getUuid());

        new While<>(msgs).all((msg, c) -> {
            String resourceUuid = msg.getVolumeUuid() != null ? msg.getVolumeUuid() : msg.getTreeUuid();
            bus.makeTargetServiceIdByResourceUuid(msg, VolumeSnapshotConstant.SERVICE_ID, resourceUuid);
            bus.send(msg, new CloudBusCallBack(c) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        c.addError(reply.getError());
                    }

                    c.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    completion.fail(errorCodeList.getCauses().get(0));
                    return;
                }

                completion.success();
            }
        });
    }

    @Override
    public void resourceOwnerAfterChange(AccountResourceRefInventory ref, String newOwnerUuid) {
        if (!VolumeVO.class.getSimpleName().equals(ref.getResourceType())) {
            return;
        }

        changeVolumeSnapshotOwner(ref, newOwnerUuid);
    }

    private void changeVolumeSnapshotOwner(AccountResourceRefInventory ref, String newOwnerUuid) {
        List<String> spUuids = Q.New(VolumeSnapshotVO.class)
                .select(VolumeSnapshotVO_.uuid)
                .eq(VolumeSnapshotVO_.volumeUuid, ref.getResourceUuid())
                .listValues();

        for (String spUuid : spUuids) {
            acntMgr.changeResourceOwner(spUuid, newOwnerUuid);
        }
    }


    @Override
    public List<Quota> reportQuota() {
        Quota quota = new Quota();
        quota.defineQuota(new VolumeSnapshotNumQuotaDefinition());
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APICreateVolumeSnapshotMsg.class)
                .addCounterQuota(VolumeSnapshotQuotaConstant.VOLUME_SNAPSHOT_NUM));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APICreateVolumeSnapshotGroupMsg.class)
                .addMessageRequiredQuotaHandler(VolumeSnapshotQuotaConstant.VOLUME_SNAPSHOT_NUM, (msg) -> SQL.New("select count(vol) from VolumeVO vol" +
                                " where vol.vmInstanceUuid =" +
                                " (select v.vmInstanceUuid from VolumeVO v where v.uuid = :uuid)", Long.class)
                        .param("uuid", msg.getRootVolumeUuid())
                        .find()));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIRecoverDataVolumeMsg.class)
                .addMessageRequiredQuotaHandler(VolumeSnapshotQuotaConstant.VOLUME_SNAPSHOT_NUM, (msg) -> Q.New(VolumeSnapshotVO.class)
                        .eq(VolumeSnapshotVO_.volumeUuid, msg.getVolumeUuid())
                        .count()));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(VolumeCreateSnapshotMsg.class)
                .addCounterQuota(VolumeSnapshotQuotaConstant.VOLUME_SNAPSHOT_NUM));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(CreateVolumeSnapshotMsg.class)
                .addCounterQuota(VolumeSnapshotQuotaConstant.VOLUME_SNAPSHOT_NUM));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(CreateVolumesSnapshotMsg.class)
                .addMessageRequiredQuotaHandler(VolumeSnapshotQuotaConstant.VOLUME_SNAPSHOT_NUM, (msg)
                        -> (long) msg.getVolumeSnapshotJobs().size()));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIChangeResourceOwnerMsg.class)
                .addCheckCondition((msg) -> Q.New(VolumeSnapshotVO.class)
                        .eq(VolumeSnapshotVO_.uuid, msg.getResourceUuid())
                        .isExists())
                .addCounterQuota(VolumeSnapshotQuotaConstant.VOLUME_SNAPSHOT_NUM));

        return list(quota);
    }

    @Override
    public void afterReimageVmInstance(VolumeInventory inventory) {
        removeVolumeFromOldSnapshotTreeInDb(inventory.getUuid());
    }

    @Override
    public void innerOverwriteVolume(VolumeInventory originVolume, VolumeInventory transientVolume, VolumeDeletionPolicyManager.VolumeDeletionPolicy originVolumeDeletionPolicy) {
        if (originVolumeDeletionPolicy != VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct) {
            return;
        }

        VolumeSnapshotReferenceUtils.handleVolumeOverwrite(originVolume, transientVolume.getUuid());
    }

    @Override
    public void afterOverwriteVolume(VolumeInventory volume, VolumeInventory transientVolume) {
        removeVolumeFromOldSnapshotTreeInDb(volume.getUuid());
    }

    @Override
    @Transactional
    public void afterFlattenVolume(VolumeInventory volume) {
        String currentSnapshotType = Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.type)
                .eq(VolumeSnapshotVO_.volumeUuid, volume.getUuid())
                .limit(1).findValue();

        if (currentSnapshotType == null || currentSnapshotType.equals(VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString())) {
            VolumeSnapshotReferenceUtils.handleStorageVolumeSnapshotReferenceAfterFlatten(volume);
        } else {
            VolumeSnapshotReferenceUtils.handleChainVolumeSnapshotReferenceAfterFlatten(volume);
        }

        removeVolumeFromOldSnapshotTreeInDb(volume.getUuid());
    }

    @Transactional
    private void removeVolumeFromOldSnapshotTreeInDb(String volumeUuid) {
        String sql = "update VolumeSnapshotVO s" +
                " set s.latest = false" +
                " where s.latest = true" +
                " and s.volumeUuid = :volumeUuid";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("volumeUuid", volumeUuid);
        q.executeUpdate();

        sql = "update VolumeSnapshotTreeVO tree" +
                " set tree.current = false" +
                " where tree.current = true" +
                " and tree.volumeUuid = :volUuid";
        q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("volUuid", volumeUuid);
        q.executeUpdate();
    }

    @Override
    public void vmJustBeforeDeleteFromDb(VmInstanceInventory inv) {
        VolumeSnapshotReferenceUtils.handleVolumeDeletion(inv.getRootVolume());
        deleteStaleSnapshotRecords(inv.getRootVolumeUuid(), VolumeType.Root, inv.getUuid());
    }

    private void deleteStaleSnapshotRecords(String volumeUuid, VolumeType volumeType, String vmUuid) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                if (!q(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.volumeUuid, volumeUuid).isExists()) {
                    return;
                }

                List<String> treeUuids = q(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.treeUuid).eq(VolumeSnapshotVO_.volumeUuid, volumeUuid).listValues();

                sql(VolumeSnapshotTreeVO.class).in(VolumeSnapshotTreeVO_.uuid, treeUuids).delete();

                sql(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.volumeUuid, volumeUuid).delete();

                if (volumeType == VolumeType.Root) {
                    sql(VolumeSnapshotGroupVO.class).eq(VolumeSnapshotGroupVO_.vmInstanceUuid, vmUuid).delete();
                } else {
                    sql(VolumeSnapshotGroupRefVO.class).eq(VolumeSnapshotGroupRefVO_.volumeUuid, volumeUuid)
                            .set(VolumeSnapshotGroupRefVO_.snapshotDeleted, true)
                            .update();
                }
            }
        }.execute();
    }

    @Override
    public void volumeJustBeforeDeleteFromDb(VolumeInventory inv) {
        VolumeSnapshotReferenceUtils.handleVolumeDeletion(inv);
        deleteStaleSnapshotRecords(inv.getUuid(), VolumeType.valueOf(inv.getType()), inv.getVmInstanceUuid());
    }
}
