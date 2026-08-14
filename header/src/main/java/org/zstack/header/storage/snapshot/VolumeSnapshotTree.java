package org.zstack.header.storage.snapshot;

import org.zstack.utils.DebugUtils;
import org.zstack.utils.function.Function;

import java.util.*;
import java.util.function.Consumer;

/**
 */
public class VolumeSnapshotTree {
    /**
     * @inventory inventory for volume snapshot tree leaf
     * @category volume snapshot
     * @example {
     * "inventory": {
     * "uuid": "59187fd8ae914927b8b3be7c51aae035",
     * "name": "Snapshot-d71b1fffebb143549dadbecd82aac998",
     * "description": "Test snapshot",
     * "type": "Hypervisor",
     * "volumeUuid": "d71b1fffebb143549dadbecd82aac998",
     * "treeUuid": "4c4fdfe0ec4b47528c23047b140ed577",
     * "hypervisorType": "KVM",
     * "primaryStorageUuid": "342ecf7e70a44f6ba81dc0533aad2b8d",
     * "primaryStorageInstallPath": "/opt/zstack/nfsprimarystorage/prim-342ecf7e70a44f6ba81dc0533aad2b8d/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-d71b1fffebb143549dadbecd82aac998/snapshots/59187fd8ae914927b8b3be7c51aae035.qcow2",
     * "type": "Root",
     * "latest": false,
     * "size": 10485760,
     * "state": "Enabled",
     * "status": "Ready",
     * "createDate": "May 3, 2014 12:17:22 PM",
     * "lastOpDate": "May 3, 2014 12:17:22 PM",
     * "backupStorageRefs": [
     * {
     * "volumeSnapshotUuid": "59187fd8ae914927b8b3be7c51aae035",
     * "backupStorageUuid": "23a96d7b4305453f9413020efaca64b2",
     * "installPath": "nfs:/test/volumeSnapshots/acct-36c27e8ff05c4780bf6d2fa65700f22e/59187fd8ae914927b8b3be7c51aae035/59187fd8ae914927b8b3be7c51aae035.qcow2"
     * }
     * ]
     * },
     * "children": [
     * {
     * "inventory": {
     * "uuid": "7ba07e804fd24a8fa6b2a3f04bb8ad94",
     * "name": "Snapshot-d71b1fffebb143549dadbecd82aac998",
     * "description": "Test snapshot",
     * "type": "Hypervisor",
     * "volumeUuid": "d71b1fffebb143549dadbecd82aac998",
     * "treeUuid": "4c4fdfe0ec4b47528c23047b140ed577",
     * "hypervisorType": "KVM",
     * "parentUuid": "59187fd8ae914927b8b3be7c51aae035",
     * "primaryStorageUuid": "342ecf7e70a44f6ba81dc0533aad2b8d",
     * "primaryStorageInstallPath": "/opt/zstack/nfsprimarystorage/prim-342ecf7e70a44f6ba81dc0533aad2b8d/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-d71b1fffebb143549dadbecd82aac998/snapshots/7ba07e804fd24a8fa6b2a3f04bb8ad94.qcow2",
     * "type": "Root",
     * "latest": false,
     * "size": 10485760,
     * "state": "Enabled",
     * "status": "Ready",
     * "createDate": "May 3, 2014 12:17:22 PM",
     * "lastOpDate": "May 3, 2014 12:17:22 PM",
     * "backupStorageRefs": [
     * {
     * "volumeSnapshotUuid": "7ba07e804fd24a8fa6b2a3f04bb8ad94",
     * "backupStorageUuid": "23a96d7b4305453f9413020efaca64b2",
     * "installPath": "nfs:/test/volumeSnapshots/acct-36c27e8ff05c4780bf6d2fa65700f22e/7ba07e804fd24a8fa6b2a3f04bb8ad94/7ba07e804fd24a8fa6b2a3f04bb8ad94.qcow2"
     * }
     * ]
     * },
     * "parentUuid": "59187fd8ae914927b8b3be7c51aae035",
     * "children": [
     * {
     * "inventory": {
     * "uuid": "e90f94533871408ab945396653208026",
     * "name": "Snapshot-d71b1fffebb143549dadbecd82aac998",
     * "description": "Test snapshot",
     * "type": "Hypervisor",
     * "volumeUuid": "d71b1fffebb143549dadbecd82aac998",
     * "treeUuid": "4c4fdfe0ec4b47528c23047b140ed577",
     * "hypervisorType": "KVM",
     * "parentUuid": "7ba07e804fd24a8fa6b2a3f04bb8ad94",
     * "primaryStorageUuid": "342ecf7e70a44f6ba81dc0533aad2b8d",
     * "primaryStorageInstallPath": "/opt/zstack/nfsprimarystorage/prim-342ecf7e70a44f6ba81dc0533aad2b8d/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-d71b1fffebb143549dadbecd82aac998/snapshots/e90f94533871408ab945396653208026.qcow2",
     * "type": "Root",
     * "latest": false,
     * "size": 10485760,
     * "state": "Enabled",
     * "status": "Ready",
     * "createDate": "May 3, 2014 12:17:22 PM",
     * "lastOpDate": "May 3, 2014 12:17:22 PM",
     * "backupStorageRefs": [
     * {
     * "volumeSnapshotUuid": "e90f94533871408ab945396653208026",
     * "backupStorageUuid": "23a96d7b4305453f9413020efaca64b2",
     * "installPath": "nfs:/test/volumeSnapshots/acct-36c27e8ff05c4780bf6d2fa65700f22e/e90f94533871408ab945396653208026/e90f94533871408ab945396653208026.qcow2"
     * }
     * ]
     * },
     * "parentUuid": "7ba07e804fd24a8fa6b2a3f04bb8ad94",
     * "children": [
     * {
     * "inventory": {
     * "uuid": "bf534fd8305d4c56aa3842b2c3dd52ab",
     * "name": "Snapshot-d71b1fffebb143549dadbecd82aac998",
     * "description": "Test snapshot",
     * "type": "Hypervisor",
     * "volumeUuid": "d71b1fffebb143549dadbecd82aac998",
     * "treeUuid": "4c4fdfe0ec4b47528c23047b140ed577",
     * "hypervisorType": "KVM",
     * "parentUuid": "e90f94533871408ab945396653208026",
     * "primaryStorageUuid": "342ecf7e70a44f6ba81dc0533aad2b8d",
     * "primaryStorageInstallPath": "/opt/zstack/nfsprimarystorage/prim-342ecf7e70a44f6ba81dc0533aad2b8d/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-d71b1fffebb143549dadbecd82aac998/snapshots/bf534fd8305d4c56aa3842b2c3dd52ab.qcow2",
     * "type": "Root",
     * "latest": true,
     * "size": 10485760,
     * "state": "Enabled",
     * "status": "Ready",
     * "createDate": "May 3, 2014 12:17:22 PM",
     * "lastOpDate": "May 3, 2014 12:17:22 PM",
     * "backupStorageRefs": [
     * {
     * "volumeSnapshotUuid": "bf534fd8305d4c56aa3842b2c3dd52ab",
     * "backupStorageUuid": "23a96d7b4305453f9413020efaca64b2",
     * "installPath": "nfs:/test/volumeSnapshots/acct-36c27e8ff05c4780bf6d2fa65700f22e/bf534fd8305d4c56aa3842b2c3dd52ab/bf534fd8305d4c56aa3842b2c3dd52ab.qcow2"
     * }
     * ]
     * },
     * "parentUuid": "e90f94533871408ab945396653208026",
     * "children": []
     * }
     * ]
     * }
     * ]
     * }
     * ]
     * }
     * @since 0.1.0
     */
    public static class SnapshotLeafInventory {
        /**
         * @desc see :ref:`VolumeSnapshotInventory`
         */
        private VolumeSnapshotInventory inventory;
        /**
         * @desc parent snapshot uuid. Null if this leaf is the tree root
         * @nullable
         */
        private String parentUuid;
        /**
         * @desc a list of children which are :ref:`SnapshotLeafInventory` as well
         */
        private List<SnapshotLeafInventory> children = new ArrayList<SnapshotLeafInventory>();

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

        public List<SnapshotLeafInventory> getChildren() {
            return children;
        }

        public void setChildren(List<SnapshotLeafInventory> children) {
            this.children = children;
        }
    }

    public static class SnapshotLeaf {
        private VolumeSnapshotInventory inventory;
        private SnapshotLeaf parent;
        private List<SnapshotLeaf> children = new ArrayList<SnapshotLeaf>();
        private List<VolumeSnapshotInventory> descendants;
        private List<VolumeSnapshotInventory> ancestors;

        public VolumeSnapshotInventory getInventory() {
            return inventory;
        }

        public void setInventory(VolumeSnapshotInventory inventory) {
            this.inventory = inventory;
        }

        public SnapshotLeaf getParent() {
            return parent;
        }

        public void setParent(SnapshotLeaf parent) {
            this.parent = parent;
        }

        public List<SnapshotLeaf> getChildren() {
            return children;
        }

        public void setChildren(List<SnapshotLeaf> children) {
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

        private static void walkDownAll(SnapshotLeaf me, Consumer<SnapshotLeaf> consumer) {
            consumer.accept(me);
            me.children.forEach(c -> walkDownAll(c, consumer));

        }
        public void walkDownAll(Consumer<SnapshotLeaf> consumer) {
            walkDownAll(this, consumer);
        }

        public VolumeSnapshotTree toSubTree() {
            VolumeSnapshotTree tree = new VolumeSnapshotTree();
            tree.root = this;
            tree.volumeUuid = inventory.getVolumeUuid();
            return tree;
        }

        private static SnapshotLeaf walkUp(SnapshotLeaf leaf, Function<Boolean, VolumeSnapshotInventory> func) {
            if (func.call(leaf.inventory)) {
                return leaf;
            }

            if (leaf.getParent() == null) {
                return null;
            }

            return walkUp(leaf.getParent(), func);
        }

        public SnapshotLeaf walkUp(Function<Boolean, VolumeSnapshotInventory> func) {
            if (func.call(inventory)) {
                return this;
            }

            if (getParent() == null) {
                return null;
            }

            return walkUp(getParent(), func);
        }

        public SnapshotLeaf walkDown(Function<Boolean, VolumeSnapshotInventory> func) {
            return walkDown(this, func);
        }

        private static SnapshotLeaf walkDown(SnapshotLeaf leaf, Function<Boolean, VolumeSnapshotInventory> func) {
            if (func.call(leaf.inventory)) {
                return leaf;
            }

            if (leaf.getChildren().isEmpty()) {
                return null;
            }

            for (SnapshotLeaf l : leaf.getChildren()) {
                SnapshotLeaf ret = walkDown(l, func);
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

                Collections.reverse(ancestors);
            }

            return ancestors;
        }

        public SnapshotLeafInventory toLeafInventory(Set<String> filterUuids) {
            return doToLeafInventory(filterUuids);
        }

        public SnapshotLeafInventory toLeafInventory() {
            return doToLeafInventory(null);
        }

        private SnapshotLeafInventory doToLeafInventory(Set<String> filterUuids) {
            SnapshotLeafInventory leafInventory = new SnapshotLeafInventory();
            leafInventory.setInventory(getInventory(filterUuids));
            if (parent != null) {
                leafInventory.setParentUuid(parent.getUuid());
            }

            for (SnapshotLeaf leaf : children) {
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
    }

    private SnapshotLeaf root;
    private String volumeUuid;
    private String uuid;

    public static VolumeSnapshotTree fromInventories(List<VolumeSnapshotInventory> invs) {
        VolumeSnapshotTree tree = new VolumeSnapshotTree();
        Map<String, SnapshotLeaf> map = new HashMap<String, SnapshotLeaf>();
        for (VolumeSnapshotInventory inv : invs) {
            SnapshotLeaf leaf = map.get(inv.getUuid());
            if (leaf == null) {
                leaf = new SnapshotLeaf();
                leaf.inventory = inv;
                map.put(inv.getUuid(), leaf);
            } else {
                leaf.inventory = inv;
            }

            if (inv.getParentUuid() != null) {
                SnapshotLeaf parent = map.get(inv.getParentUuid());
                if (parent == null) {
                    parent = new SnapshotLeaf();
                    parent.setUuid(inv.getParentUuid());
                    map.put(parent.getUuid(), parent);
                }

                parent.children.add(leaf);
                leaf.parent = parent;
            } else {
                tree.root = leaf;
            }

            tree.volumeUuid = inv.getVolumeUuid();
            tree.uuid = inv.getTreeUuid();
        }

        DebugUtils.Assert(tree.root != null, "why tree root is null???");
        return tree;
    }

    public static VolumeSnapshotTree fromVOs(List<VolumeSnapshotVO> vos) {
        return fromInventories(VolumeSnapshotInventory.valueOf(vos));
    }

    public SnapshotLeaf getRoot() {
        return root;
    }

    public void setRoot(SnapshotLeaf root) {
        this.root = root;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    private SnapshotLeaf findSnapshot(final List<SnapshotLeaf> leafs, final Function<Boolean, VolumeSnapshotInventory> func) {
        for (SnapshotLeaf leaf : leafs) {
            SnapshotLeaf ret = findSnapshot(leaf.children, func);
            if (ret != null) {
                return ret;
            }

            if (func.call(leaf.getInventory())) {
                return leaf;
            }
        }

        return null;
    }

    public SnapshotLeaf findSnapshot(Function<Boolean, VolumeSnapshotInventory> func) {
        if (func.call(root.getInventory())) {
            return root;
        }
        return findSnapshot(root.children, func);
    }

    public SnapshotLeaf findSnapshot(String snapshotUuid) {
        if (snapshotUuid == null) {
            return null;
        }

        return findSnapshot(arg -> arg.getUuid().equals(snapshotUuid));
    }
}
