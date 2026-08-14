package org.zstack.storage.snapshot;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO_;
import org.zstack.header.storage.snapshot.reference.VolumeSnapshotReferenceVO;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * When building the VolumeTree, if the tree is current, then add the volume to the VolumeTree.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeTree {
    private static final CLogger logger = Utils.getLogger(VolumeTree.class);

    @Autowired
    private DatabaseFacade dbf;

    private VolumeSnapshotLeaf root;
    VolumeInventory volume;
    List<VolumeSnapshotInventory> allSnapshots = new ArrayList<>();
    private boolean current;

    public boolean isCurrent() {
        return current;
    }

    // the aliveChainSnapshots represents a chain where the volume is located, but only if the VolumeTree contains that volume.
    // If the VolumeTree does not contain the volume, then aliveChainInventory should be empty.
    private List<VolumeSnapshotInventory> aliveChain = new ArrayList<>();

    private static class VolumeSnapshotLeafInventory {
        private VolumeSnapshotInventory inventory;
        private String parentUuid;
        private List<VolumeSnapshotLeafInventory> children = new ArrayList<>();

        public VolumeSnapshotInventory getInventory() {
            return inventory;
        }

        public void setInventory(VolumeSnapshotInventory inventory) {
            this.inventory = inventory;
        }

        public String getParentUuid() {
            return parentUuid;
        }

        public void setParentUuid(String parentUuid) {
            this.parentUuid = parentUuid;
        }

        public List<VolumeSnapshotLeafInventory> getChildren() {
            return children;
        }

        public void setChildren(List<VolumeSnapshotLeafInventory> children) {
            this.children = children;
        }

        public String getStatus() {
            return inventory.getStatus();
        }
    }

    public static class VolumeSnapshotLeaf {
        private VolumeSnapshotInventory inventory;
        private VolumeSnapshotLeaf parent;
        private List<VolumeSnapshotLeaf> children = new ArrayList<>();
        private List<VolumeSnapshotInventory> descendants;
        private List<VolumeSnapshotInventory> ancestors;

        public VolumeSnapshotInventory getInventory() {
            return inventory;
        }

        public void setInventory(VolumeSnapshotInventory inventory) {
            this.inventory = inventory;
        }

        public VolumeSnapshotLeaf getParent() {
            return parent;
        }

        public void setParent(VolumeSnapshotLeaf parent) {
            this.parent = parent;
        }

        public List<VolumeSnapshotLeaf> getChildren() {
            return children;
        }

        public void setChildren(List<VolumeSnapshotLeaf> children) {
            this.children = children;
        }

        public String getUuid() {
            return inventory.getUuid();
        }

        public void setUuid(String uuid) {
            if (inventory == null) {
                inventory = new VolumeSnapshotInventory();
            }
            inventory.setUuid(uuid);
        }

        private static void walkDownAll(VolumeSnapshotLeaf me, Consumer<VolumeSnapshotLeaf> consumer) {
            consumer.accept(me);
            me.children.forEach(c -> walkDownAll(c, consumer));

        }

        public void walkDownAll(Consumer<VolumeSnapshotLeaf> consumer) {
            walkDownAll(this, consumer);
        }

        public VolumeTree toSubTree() {
            VolumeTree tree = new VolumeTree();
            tree.root = this;
            return tree;
        }

        private static VolumeSnapshotLeaf walkUp(VolumeSnapshotLeaf leaf, Function<Boolean, VolumeSnapshotInventory> func) {
            if (func.call(leaf.inventory)) {
                return leaf;
            }

            if (leaf.getParent() == null) {
                return null;
            }

            return walkUp(leaf.getParent(), func);
        }

        public VolumeSnapshotLeaf walkUp(Function<Boolean, VolumeSnapshotInventory> func) {
            if (func.call(inventory)) {
                return this;
            }

            if (getParent() == null) {
                return null;
            }

            return walkUp(getParent(), func);
        }

        public VolumeSnapshotLeaf walkDown(Function<Boolean, VolumeSnapshotInventory> func) {
            return walkDown(this, func);
        }

        private static VolumeSnapshotLeaf walkDown(VolumeSnapshotLeaf leaf, Function<Boolean, VolumeSnapshotInventory> func) {
            if (func.call(leaf.inventory)) {
                return leaf;
            }

            if (leaf.getChildren().isEmpty()) {
                return null;
            }

            for (VolumeSnapshotLeaf l : leaf.getChildren()) {
                VolumeSnapshotLeaf ret = walkDown(l, func);
                if (ret != null) {
                    return ret;
                }
            }

            return null;
        }

        public List<VolumeSnapshotInventory> getDescendants() {
            if (descendants == null) {
                descendants = new ArrayList<VolumeSnapshotInventory>();
                walkDown(new Function<Boolean, VolumeSnapshotInventory>() {
                    @Override
                    public Boolean call(VolumeSnapshotInventory arg) {
                        descendants.add(arg);
                        return false;
                    }
                });
            }

            return descendants;
        }

        public List<VolumeSnapshotInventory> getAncestors() {
            if (ancestors == null) {
                ancestors = new ArrayList<VolumeSnapshotInventory>();
                walkUp(new Function<Boolean, VolumeSnapshotInventory>() {
                    @Override
                    public Boolean call(VolumeSnapshotInventory arg) {
                        ancestors.add(arg);
                        return false;
                    }
                });
            }

            return ancestors;
        }

        public VolumeSnapshotLeafInventory toLeafInventory(Set<String> filterUuids) {
            return doToLeafInventory(filterUuids);
        }

        public VolumeSnapshotLeafInventory toLeafInventory() {
            return doToLeafInventory(null);
        }

        private VolumeSnapshotLeafInventory doToLeafInventory(Set<String> filterUuids) {
            VolumeSnapshotLeafInventory leafInventory = new VolumeSnapshotLeafInventory();
            leafInventory.setInventory(getInventory(filterUuids));
            if (parent != null) {
                leafInventory.setParentUuid(parent.getUuid());
            }

            for (VolumeSnapshotLeaf leaf : children) {
                leafInventory.getChildren().add(leaf.doToLeafInventory(filterUuids));
            }

            return leafInventory;
        }

        private VolumeSnapshotInventory getInventory(Set<String> filterUuids) {
            if (filterUuids == null || filterUuids.contains(inventory.getUuid())) {
                return inventory;
            } else {
                VolumeSnapshotInventory inv = new VolumeSnapshotInventory();
                inv.setUuid(inventory.getUuid());
                return inv;
            }
        }

        public List<String> getChildrenVolumeSnapshotInventoryUuid() {
            return children.stream().map(it -> it.getInventory().getUuid()).collect(Collectors.toList());
        }

        public String getStatus() {
            return inventory.getStatus();
        }
    }

    // TODO(clone) : When both chain cloning and single-node snapshot deletion are enabled,
    //  it is necessary to consider the dependency relationships of all snapshot nodes in the current snapshot tree within the VolumeSnapshotReferenceVO.
    public static VolumeTree fromVOs(List<VolumeSnapshotVO> vos, boolean current, VolumeInventory volumeInv) {
        List<VolumeSnapshotVO> noParentVO = vos.stream().filter(it -> it.getParentUuid() == null).collect(Collectors.toList());
        if (noParentVO.size() > 1) {
            throw new IllegalArgumentException(String.format("There are %d root snapshots on tree[uuid:%s]",
                    noParentVO.size(), vos.get(0).getTreeUuid()));
        }

        List<VolumeSnapshotInventory> invs = VolumeSnapshotInventory.valueOf(vos);

        List<VolumeSnapshotInventory> latestSnapshotInv = invs.stream()
                .filter(VolumeSnapshotInventory::isLatest).collect(Collectors.toList());
        if (latestSnapshotInv.size() > 1) {
            throw new IllegalArgumentException(String.format("There are %d latest snapshots on tree[uuid:%s]",
                    latestSnapshotInv.size(), invs.get(0).getTreeUuid()));
        }

        VolumeTree tree = new VolumeTree();
        tree.current = current;
        tree.volume = volumeInv;
        if (tree.current && latestSnapshotInv.size() == 1) {
            VolumeSnapshotVO snapshotVO = new VolumeSnapshotVO();
            snapshotVO.setLatest(false);
            snapshotVO.setName(String.format("volume-%s-%s", volumeInv.getName(), volumeInv.getUuid()));
            snapshotVO.setUuid(volumeInv.getUuid());
            VolumeSnapshotInventory latestInv = latestSnapshotInv.get(0);
            snapshotVO.setParentUuid(latestInv.getUuid());
            snapshotVO.setTreeUuid(latestInv.getTreeUuid());
            snapshotVO.setState(VolumeSnapshotState.Enabled);
            snapshotVO.setStatus(VolumeSnapshotStatus.Ready);
            snapshotVO.setPrimaryStorageInstallPath(volumeInv.getInstallPath());
            snapshotVO.setPrimaryStorageUuid(volumeInv.getPrimaryStorageUuid());
            invs.add(VolumeSnapshotInventory.valueOf(snapshotVO));
        }

        Map<String, VolumeSnapshotLeaf> map = new HashMap<>();
        for (VolumeSnapshotInventory inv : invs) {
            VolumeSnapshotLeaf leaf = map.get(inv.getUuid());
            if (leaf == null) {
                leaf = new VolumeSnapshotLeaf();
                leaf.inventory = inv;
                map.put(inv.getUuid(), leaf);
            } else {
                leaf.inventory = inv;
            }

            if (inv.getParentUuid() != null) {
                VolumeSnapshotLeaf parent = map.get(inv.getParentUuid());
                if (parent == null) {
                    parent = new VolumeSnapshotLeaf();
                    parent.setUuid(inv.getParentUuid());
                    map.put(parent.getUuid(), parent);
                }

                parent.children.add(leaf);
                leaf.parent = parent;
            } else {
                tree.root = leaf;
            }
        }

        if (tree.current) {
            VolumeSnapshotLeaf leaf = tree.getSnapshotLeaf(volumeInv.getUuid());
            tree.aliveChain = leaf != null ? leaf.getAncestors() : new ArrayList<>();
        }
        DebugUtils.Assert(tree.root != null, "why tree root is null???");
        tree.allSnapshots = invs;
        return tree;
    }

    private VolumeSnapshotLeaf findSnapshot(final List<VolumeSnapshotLeaf> leafs, final Function<Boolean, VolumeSnapshotInventory> func) {
        for (VolumeSnapshotLeaf leaf : leafs) {
            VolumeSnapshotLeaf ret = findSnapshot(leaf.children, func);
            if (ret != null) {
                return ret;
            }

            if (func.call(leaf.getInventory())) {
                return leaf;
            }
        }

        return null;
    }

    private VolumeSnapshotLeaf findSnapshot(Function<Boolean, VolumeSnapshotInventory> func) {
        if (func.call(root.getInventory())) {
            return root;
        }
        return findSnapshot(root.children, func);
    }

    public VolumeSnapshotLeaf getSnapshotLeaf(String snapshotUuid) {
        return findSnapshot(new Function<Boolean, VolumeSnapshotInventory>() {
            @Override
            public Boolean call(VolumeSnapshotInventory arg) {
                return arg.getUuid().equals(snapshotUuid);
            }
        });
    }

    public List<String> getAliveChainSnapshotUuids() {
        return aliveChain.stream().map(VolumeSnapshotInventory::getUuid).collect(Collectors.toList());
    }

    public DeleteVolumeSnapshotDirection resolveDirection(String targetSnapshotUuid, String childSnapshotUuid, String initialDirection,
                                                          boolean targetSnapshotIsLatest, VmInstanceState vmState) {
        boolean online = (vmState == VmInstanceState.Running || vmState == VmInstanceState.Paused)
                && getAliveChainSnapshotUuids().contains(targetSnapshotUuid) && getAliveChainSnapshotUuids().contains(childSnapshotUuid);

        boolean shouldUseCommitStrategy = current && !targetSnapshotIsLatest && online;

        if (Objects.equals(initialDirection, DeleteVolumeSnapshotDirection.Pull.toString()) && shouldUseCommitStrategy) {
            throw new IllegalArgumentException("the snapshot will be deleted by block 'commit', but the direction is 'pull', " +
                    "change the direction to 'commit' or 'auto'.");
        }

        if (initialDirection == null) {
            return DeleteVolumeSnapshotDirection.Commit;
        }

        if (Objects.equals(initialDirection, DeleteVolumeSnapshotDirection.Auto.toString())) {
            return shouldUseCommitStrategy ?
                    DeleteVolumeSnapshotDirection.Commit :
                    DeleteVolumeSnapshotDirection.Pull;
        }

        return DeleteVolumeSnapshotDirection.fromString(initialDirection);
    }

    public boolean isOnline(boolean treeIsCurrent, String targetSnapshotUuid, String childSnapshotUuid, VmInstanceState vmState) {
        return treeIsCurrent && (vmState == VmInstanceState.Running || vmState == VmInstanceState.Paused)
                && getAliveChainSnapshotUuids().contains(targetSnapshotUuid) && getAliveChainSnapshotUuids().contains(childSnapshotUuid);
    }

    // TODO(clone) : When both chain cloning and single-node snapshot deletion are enabled,
    //  the following three functions must take into account the dependencies within the snapshot chain.
    public void updateDatabaseAfterPullToVolume(VolumeSnapshotInventory srcSnapshotInv) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, srcSnapshotInv.getUuid())
                        .set(VolumeSnapshotVO_.latest, false).update();

                if (srcSnapshotInv.getParentUuid() != null) {
                    sql(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, srcSnapshotInv.getParentUuid())
                            .set(VolumeSnapshotVO_.latest, true).update();
                    logger.debug(String.format("reset latest snapshot of tree[uuid:%s] to snapshot[uuid:%s]",
                            srcSnapshotInv.getTreeUuid(), srcSnapshotInv.getParentUuid()));
                }

                if (srcSnapshotInv.getParentUuid() == null) {
                    sql(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.uuid, srcSnapshotInv.getTreeUuid())
                            .set(VolumeSnapshotTreeVO_.current, false).update();
                }
            }
        }.execute();
    }

    public void updateDatabaseAfterPull(VolumeSnapshotInventory srcSnapshotInv, VolumeTree.VolumeSnapshotLeaf dstSnapshotLeaf, long newInstallPathSize) {
        VolumeSnapshotInventory dstSnapshotInv = dstSnapshotLeaf.getInventory();

        new SQLBatch() {
            @Override
            protected void scripts() {
                List<String> descendantsUuid = dstSnapshotLeaf.getDescendants().stream().map(VolumeSnapshotInventory::getUuid)
                        .filter(uuid -> !Objects.equals(uuid, volume.getUuid()))
                        .collect(Collectors.toList());

                List<VolumeSnapshotVO> vos = q(VolumeSnapshotVO.class).in(VolumeSnapshotVO_.uuid, descendantsUuid).list();
                vos.forEach(vo -> {
                    // update distance
                    vo.setDistance(vo.getDistance() - 1);

                    if (Objects.equals(vo.getUuid(), dstSnapshotInv.getUuid())) {
                        // update parentUuid
                        vo.setParentUuid(srcSnapshotInv.getParentUuid());
                        logger.debug(String.format("update the parent of snapshot[uuid:%s] to %s", dstSnapshotInv.getUuid(), srcSnapshotInv.getParentUuid()));

                        vo.setSize(newInstallPathSize);
                        logger.debug(String.format("update the size of snapshot[uuid:%s] to %s", dstSnapshotInv.getUuid(), newInstallPathSize));
                    }
                });

                VolumeSnapshotTreeVO newTree = null;
                // create new tree and update treeUuid
                if (srcSnapshotInv.getParentUuid() == null) {
                    newTree = new VolumeSnapshotTreeVO();
                    newTree.setCurrent(descendantsUuid.contains(volume.getUuid()));
                    newTree.setVolumeUuid(volume.getUuid());
                    newTree.setUuid(Platform.getUuid());
                    newTree.setStatus(VolumeSnapshotTreeStatus.Completed);
                    if (getAliveChainSnapshotUuids().contains(dstSnapshotInv.getUuid())) {
                        newTree.setCurrent(true);
                    }
                    dbf.persist(newTree);
                    logger.debug(String.format("created new volume snapshot tree[uuid:%s]", newTree.getUuid()));
                    VolumeSnapshotTreeVO finalNewTree = newTree;
                    vos.forEach(vo -> vo.setTreeUuid(finalNewTree.getUuid()));
                }

                dbf.updateCollection(vos);

                if (newTree != null && Objects.equals(dstSnapshotInv.getUuid(), volume.getUuid())
                        && q(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.uuid, srcSnapshotInv.getTreeUuid()).count() == 1) {
                    sql(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.uuid, srcSnapshotInv.getTreeUuid())
                            .set(VolumeSnapshotTreeVO_.current, false).update();
                }
            }
        }.execute();
    }

    public void updateDatabaseAfterCommit(VolumeTree.VolumeSnapshotLeaf srcSnapshotLeaf, VolumeSnapshotInventory dstSnapshotInv, long newInstallPathSize) {
        VolumeSnapshotInventory srcSnapshotInv = srcSnapshotLeaf.getInventory();
        new SQLBatch() {
            @Override
            protected void scripts() {
                List<String> descendantsUuid = srcSnapshotLeaf.getDescendants().stream().map(VolumeSnapshotInventory::getUuid)
                        .filter(uuid -> !Objects.equals(uuid, srcSnapshotLeaf.getUuid()))
                        .filter(uuid -> !Objects.equals(uuid, volume.getUuid()))
                        .collect(Collectors.toList());

                List<VolumeSnapshotVO> vos = new ArrayList<>();
                if (!descendantsUuid.isEmpty()) {
                    vos = Q.New(VolumeSnapshotVO.class).in(VolumeSnapshotVO_.uuid, descendantsUuid).list();
                    vos.forEach(vo -> {
                        // update distance
                        vo.setDistance(vo.getDistance() - 1);
                    });
                }

                VolumeSnapshotTreeVO newTree = null;
                // create new tree and update treeUuid
                if (dstSnapshotInv.getParentUuid() == null) {
                    newTree = new VolumeSnapshotTreeVO();
                    newTree.setCurrent(descendantsUuid.contains(volume.getUuid()));
                    newTree.setVolumeUuid(volume.getUuid());
                    newTree.setUuid(Platform.getUuid());
                    newTree.setStatus(VolumeSnapshotTreeStatus.Completed);
                    if (getAliveChainSnapshotUuids().contains(srcSnapshotInv.getUuid())) {
                        newTree.setCurrent(true);
                    }
                    dbf.persist(newTree);
                    logger.debug(String.format("created new volume snapshot tree[uuid:%s]", newTree.getUuid()));
                }
                if (!vos.isEmpty() && newTree != null) {
                    VolumeSnapshotTreeVO finalNewTree = newTree;
                    vos.forEach(vo -> vo.setTreeUuid(finalNewTree.getUuid()));
                    dbf.updateCollection(vos);
                }

                if (newTree != null && Objects.equals(srcSnapshotInv.getUuid(), volume.getUuid())
                        && q(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.uuid, dstSnapshotInv.getTreeUuid()).count() == 1) {
                    sql(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.uuid, srcSnapshotInv.getTreeUuid())
                            .set(VolumeSnapshotTreeVO_.current, false).update();
                }

                sql(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, dstSnapshotInv.getUuid())
                        .set(VolumeSnapshotVO_.primaryStorageInstallPath, srcSnapshotInv.getPrimaryStorageInstallPath())
                        .set(VolumeSnapshotVO_.size, srcSnapshotInv.getSize()).update();
                if (dstSnapshotInv.getGroupUuid() != null) {
                    sql(VolumeSnapshotGroupRefVO.class)
                            .eq(VolumeSnapshotGroupRefVO_.volumeSnapshotGroupUuid, dstSnapshotInv.getGroupUuid())
                            .eq(VolumeSnapshotGroupRefVO_.volumeSnapshotUuid, dstSnapshotInv.getUuid())
                            .set(VolumeSnapshotGroupRefVO_.volumeSnapshotInstallPath, srcSnapshotInv.getPrimaryStorageInstallPath())
                            .update();
                }

                sql(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, srcSnapshotInv.getUuid())
                        .set(VolumeSnapshotVO_.primaryStorageInstallPath, dstSnapshotInv.getPrimaryStorageInstallPath())
                        .set(VolumeSnapshotVO_.size, newInstallPathSize)
                        .set(VolumeSnapshotVO_.distance, srcSnapshotInv.getDistance() - 1)
                        .set(VolumeSnapshotVO_.parentUuid, dstSnapshotInv.getParentUuid())
                        .set(VolumeSnapshotVO_.treeUuid, newTree != null ? newTree.getUuid() : srcSnapshotInv.getTreeUuid())
                        .update();
                logger.debug(String.format("update the parent of snapshot[uuid:%s] to %s", srcSnapshotInv.getUuid(), dstSnapshotInv.getParentUuid()));

                if (srcSnapshotInv.getGroupUuid() != null) {
                    sql(VolumeSnapshotGroupRefVO.class)
                            .eq(VolumeSnapshotGroupRefVO_.volumeSnapshotGroupUuid, srcSnapshotInv.getGroupUuid())
                            .eq(VolumeSnapshotGroupRefVO_.volumeSnapshotUuid, srcSnapshotInv.getUuid())
                            .set(VolumeSnapshotGroupRefVO_.volumeSnapshotInstallPath, dstSnapshotInv.getPrimaryStorageInstallPath())
                            .update();
                }
            }
        }.execute();
    }
}
