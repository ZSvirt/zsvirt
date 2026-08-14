package org.zstack.storage.snapshot.reference;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.trash.StorageTrash;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.DeleteVolumeChainOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.GetVolumeBackingChainFromPrimaryStorageMsg;
import org.zstack.header.storage.primary.GetVolumeBackingChainFromPrimaryStorageReply;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.snapshot.reference.DeleteVolumeSnapshotReferenceLeafMsg;
import org.zstack.header.storage.snapshot.reference.DeleteVolumeSnapshotReferenceLeafReply;
import org.zstack.header.storage.snapshot.reference.VolumeSnapshotReferenceInventory;
import org.zstack.header.storage.snapshot.reference.VolumeSnapshotReferenceTreeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;


@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeSnapshotReferenceTreeBase {
    private static final CLogger logger = Utils.getLogger(VolumeSnapshotReferenceTreeBase.class);

    @Autowired
    private CloudBus bus;

    @Autowired
    private ThreadFacade thdf;

    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    private StorageTrash trash;

    private VolumeSnapshotReferenceTreeInventory self;

    public VolumeSnapshotReferenceTreeBase(VolumeSnapshotReferenceTreeInventory self) {
        this.self = self;
        this.id = "VolumeSnapshotReferenceTree-" + self.getUuid();
    }

    protected final String id;

    public void handleMessage(Message msg) {
        if (msg instanceof DeleteVolumeSnapshotReferenceLeafMsg) {
            handle((DeleteVolumeSnapshotReferenceLeafMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(DeleteVolumeSnapshotReferenceLeafMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                DeleteVolumeSnapshotReferenceLeafReply reply = new DeleteVolumeSnapshotReferenceLeafReply();
                deleteSnapshotRefLeaf(msg, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public String getName() {
                return "delete-volume-snapshot-reference-leaf";
            }
        });
    }

    private void deleteSnapshotRefLeaf(DeleteVolumeSnapshotReferenceLeafMsg msg, Completion completion) {
        Set<String> otherLeafDirectBackingInstallUrls = msg.getOtherLeafs().stream()
                .map(VolumeSnapshotReferenceInventory::getDirectSnapshotInstallUrl)
                .collect(Collectors.toSet());
        if (otherLeafDirectBackingInstallUrls.contains(msg.getLeaf().getDirectSnapshotInstallUrl())) {
            logger.debug(String.format("other leafs has the same direct backing, skip delete leaf: [%d]", msg.getLeaf().getId()));
            completion.success();
            return;
        }

        boolean rootDeleted = msg.getLeaf().getParentId() == null && !Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, msg.getLeaf().getVolumeUuid())
                .eq(VolumeVO_.primaryStorageUuid, msg.getTree().getPrimaryStorageUuid())
                .isExists();
        String endPath = rootDeleted ? self.getRootInstallUrl() : msg.getLeaf().getVolumeSnapshotInstallUrl();
        String startPath = msg.getLeaf().getDirectSnapshotInstallUrl();
        if (startPath.equals(endPath) && !rootDeleted) {
            logger.debug(String.format("no volume between leaf: [%d] and its parent, skip delete leaf", msg.getLeaf().getId()));
            completion.success();
            return;
        } else {
            logger.debug(String.format("it is going to delete untracked volume resource preliminarily: " +
                    "startPath: [%s], endPath: [%s], contains end: %s", startPath, endPath, rootDeleted));
        }

        List<String> directBackingInstallUrls = new ArrayList<>(otherLeafDirectBackingInstallUrls);
        directBackingInstallUrls.add(msg.getLeaf().getDirectSnapshotInstallUrl());
        final Map<String, List<String>> backingChains = new HashMap<>();
        final List<String> toDeletePaths = new ArrayList<>();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("delete-volume-snapshot-reference-leaf");
        chain.then(new Flow() {
            String __name__ = "get-leafs-backing-chain";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                GetVolumeBackingChainFromPrimaryStorageMsg gmsg = new GetVolumeBackingChainFromPrimaryStorageMsg();
                gmsg.setVolumeUuid(msg.getDeletedVolume().getUuid());
                gmsg.setRootInstallPaths(directBackingInstallUrls);
                gmsg.setPrimaryStorageUuid(msg.getTree().getPrimaryStorageUuid());
                gmsg.setHostUuid(msg.getTree().getHostUuid());
                gmsg.setVolumeFormat(msg.getDeletedVolume().getFormat());
                bus.makeTargetServiceIdByResourceUuid(gmsg, PrimaryStorageConstant.SERVICE_ID, gmsg.getPrimaryStorageUuid());
                bus.send(gmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        GetVolumeBackingChainFromPrimaryStorageReply gr = reply.castReply();
                        for (Map.Entry<String, List<String>> e : gr.getBackingChainInstallPath().entrySet()) {
                            backingChains.put(e.getKey(), new ArrayList<>(e.getValue()));
                            if (!e.getValue().contains(e.getKey())) {
                                backingChains.get(e.getKey()).add(0, e.getKey());
                            }
                        }
                        trigger.next();
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                trigger.rollback();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "ensure-leafs-are-not-in-use";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                for (String path : backingChains.get(startPath)) {
                    if (path.contains(endPath) && !rootDeleted) {
                        break;
                    }

                    String usedDetails = trash.makeSurePrimaryStorageInstallPathNotUsed(path);
                    if (usedDetails != null) {
                        logger.error("[THIS IS A BUG NEEDED TO BE FIXED RIGHT NOW, PLEASE REPORT TO US ASAP]" +
                                " install path[" + path + "] is still used, details: " + usedDetails);
                        break;
                    }

                    toDeletePaths.add(path);
                }

                Set<String> usedByOthersPaths = backingChains.entrySet().stream()
                        .filter(e -> !e.getKey().equals(startPath))
                        .flatMap(e -> e.getValue().stream()).collect(Collectors.toSet());

                toDeletePaths.removeAll(usedByOthersPaths);
                logger.debug("delete snapshot reference leafs: " + toDeletePaths);
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "delete-leafs-on-primary-storage";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                DeleteVolumeChainOnPrimaryStorageMsg dmsg = new DeleteVolumeChainOnPrimaryStorageMsg();
                dmsg.setInstallPaths(toDeletePaths);
                dmsg.setPrimaryStorageUuid(msg.getTree().getPrimaryStorageUuid());
                dmsg.setHostUuid(msg.getTree().getHostUuid());
                dmsg.setVolumeFormat(msg.getDeletedVolume().getFormat());
                bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, dmsg.getPrimaryStorageUuid());
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        trigger.next();
                    }
                });
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).start();
    }

    private boolean hasSameVolumeResource(String snapshotInstallUrl, Set<String> snapshotInstallUrls) {
        if (!snapshotInstallUrl.contains("@")) {
            return snapshotInstallUrls.contains(snapshotInstallUrl);
        }

        String volumeInstallUrl = getVolumeInstallUrl(snapshotInstallUrl);
        for (String url : snapshotInstallUrls) {
            if (getVolumeInstallUrl(url).equals(volumeInstallUrl)) {
                return true;
            }
        }

        return false;
    }

    private void filterDeletePaths(List<String> toDeletePaths, Set<String> protectedPaths) {
        Iterator<String> it = toDeletePaths.iterator();
        Set<String> protectedVolumePaths = protectedPaths.stream().map(this::getVolumeInstallUrl).collect(Collectors.toSet());
        while (it.hasNext()) {
            String path = getVolumeInstallUrl(it.next());
            if (protectedVolumePaths.contains(path)) {
                it.remove();
            }
        }
    }

    // TODO hard code for internal snapshot, need to be refactored
    private String getVolumeInstallUrl(String snapshotInstallUrl) {
        if (!snapshotInstallUrl.contains("@")) {
            return snapshotInstallUrl;
        }

        return snapshotInstallUrl.split("@")[0];
    }
}
