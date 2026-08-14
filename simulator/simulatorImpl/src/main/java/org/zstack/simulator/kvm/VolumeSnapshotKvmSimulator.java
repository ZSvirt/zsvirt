package org.zstack.simulator.kvm;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotTree;
import org.zstack.header.storage.snapshot.VolumeSnapshotTree.SnapshotLeaf;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.kvm.KVMAgentCommands.TakeSnapshotCmd;
import org.zstack.kvm.KVMAgentCommands.TakeSnapshotResponse;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands.RevertVolumeFromSnapshotCmd;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands.RevertVolumeFromSnapshotResponse;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 */
public class VolumeSnapshotKvmSimulator {
    private static CLogger logger = Utils.getLogger(VolumeSnapshotKvmSimulator.class);

    @Autowired
    private DatabaseFacade dbf;

    public static class Qcow2 {
        private String installPath;
        private List<Qcow2> next = new ArrayList<Qcow2>();
        private Qcow2 prev;

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public void addNext(Qcow2 n) {
            next.add(n);
        }

        public void deleteNext(Qcow2 n) {
            next.remove(n);
        }

        public List<Qcow2> getNext() {
            return next;
        }

        public void setNext(List<Qcow2> next) {
            this.next = next;
        }

        public Qcow2 getPrev() {
            return prev;
        }

        public void setPrev(Qcow2 prev) {
            this.prev = prev;
        }

        private Qcow2 walkDown(Qcow2 s, Function<Qcow2, Qcow2> func) {
            Qcow2 ret = null;

            if (s == null) {
                return null;
            }

            ret = func.call(s);
            if (ret != null) {
                return ret;
            }

            if (s.next.isEmpty()) {
                return null;
            }

            for (Qcow2 q : s.next) {
                ret = walkDown(q, func);
                if (ret != null) {
                    return ret;
                }
            }

            return ret;
        }

        public Qcow2 walkDown(Function<Qcow2, Qcow2> func) {
            return walkDown(this, func);
        }

        private Qcow2 walkUp(Qcow2 s, Function<Qcow2, Qcow2> func) {
            Qcow2 ret = null;

            if (s == null) {
                return null;
            }

            ret = func.call(s);
            if (ret != null) {
                return ret;
            }

            if (s.prev == null) {
                return null;
            }

            return walkDown(s.prev, func);
        }

        public Qcow2 walkUp(Function<Qcow2, Qcow2> func) {
            return walkUp(this, func);
        }

        public Qcow2 find(final String installPath) {
            return walkDown(new Function<Qcow2, Qcow2>() {
                @Override
                public Qcow2 call(Qcow2 arg) {
                    if (installPath.equals(arg.getInstallPath())) {
                        return arg;
                    }
                    return null;
                }
            });
        }

        public static Qcow2 newQcow2(String installPath) {
            return newQcow2(installPath, null);
        }

        public static Qcow2 newQcow2(String installPath, Qcow2 parent) {
            DebugUtils.Assert(installPath!=null, "installPath cannot be null");
            Qcow2 s = new Qcow2();
            s.setInstallPath(installPath);
            if (parent != null) {
                parent.addNext(s);
                s.setPrev(parent);
            }
            return s;
        }

        private Qcow2 findRoot(Qcow2 qcow2) {
            if (qcow2.prev == null) {
                return qcow2;
            }

            return findRoot(qcow2.prev);
        }

        public Qcow2 findRoot() {
            return findRoot(this);
        }

        public void print(PrintWriter writer) {
            print(writer, "", true);
        }

        private void print(PrintWriter writer, String prefix, boolean isTail) {
            writer.println(prefix + (isTail ? "|__ " : "|---") + installPath);
            for (int i = 0; i < next.size() - 1; i++) {
                next.get(i).print(writer, prefix + (isTail ? "    " : "|   "), false);
            }
            if (next.size() >= 1) {
                next.get(next.size() - 1).print(writer, prefix + (isTail ? "    " : "|   "), true);
            }
        }
    }

    private Map<String, Qcow2> snapshots = new HashMap<String, Qcow2>();

    private Qcow2 findByInstallPath(String installPath) {
        for (Qcow2 s : snapshots.values()) {
            Qcow2 ret = s.find(installPath);
            if (ret != null) {
                return ret;
            }
        }

        return null;
    }

    private void dumpQcow2Tree(Qcow2 current, String msg) {
        Qcow2 root = current.findRoot();
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        printWriter.println(String.format("======== %s ==============", msg));
        root.print(printWriter);
        printWriter.println(String.format("======== End of %s =======", msg));
        logger.debug("\n\n" + stringWriter.toString());
    }

    public synchronized TakeSnapshotResponse takeSnapshot(TakeSnapshotCmd cmd) {
        Qcow2 current = findByInstallPath(cmd.getVolumeInstallPath());
        if (cmd.isFullSnapshot() && current == null) {
            dumpAllQcow2();
            String err = String.format("cannot find snapshot[%s] and it's full snapshot", cmd.getVolumeInstallPath());
            DebugUtils.dumpStackTrace(err);
            DebugUtils.Assert(false, err);
        }

        if (current == null) {
            current = Qcow2.newQcow2(cmd.getVolumeInstallPath());
            snapshots.put(current.getInstallPath(), current);
        } else {
            if (cmd.isFullSnapshot()) {
                String dir = PathUtil.parentFolder(cmd.getVolumeInstallPath());
                String newVolumePath = String.format("%s/%s.qcow2", dir, Platform.getUuid());
                current = Qcow2.newQcow2(newVolumePath);
                snapshots.put(current.getInstallPath(), current);
            }
        }

        Qcow2 n = Qcow2.newQcow2(cmd.getInstallPath(), current);

        logger.debug(String.format("created new volume[%s] for taking snapshot", n.getInstallPath()));
        dumpQcow2Tree(current, "Taking Snapshot");

        TakeSnapshotResponse rsp = new TakeSnapshotResponse();
        rsp.setSize(100000);
        rsp.setNewVolumeInstallPath(n.getInstallPath());
        rsp.setSnapshotInstallPath(current.getInstallPath());
        return rsp;
    }

    private void dumpAllQcow2() {
        int i = 0;
        for (Qcow2 q : snapshots.values()) {
            logger.debug("\n ------------------ All snapshots dump ------------------- \n");
            dumpQcow2Tree(q, String.valueOf(i++));
            logger.debug("\n ------------------ End of all snapshots dump ------------ \n");
        }
    }

    public synchronized void merge(String src, String dest, boolean fullRebase) {
        Qcow2 qsrc = findByInstallPath(src);
        DebugUtils.Assert(qsrc!=null || fullRebase, String.format("cannot find source snapshot[%s]", src));
        Qcow2 qdest = qsrc.find(dest);
        if (qdest == null) {
            dumpAllQcow2();
            DebugUtils.Assert(false, String.format("cannot find target volume[%s] to merge", dest));
        }

        if (fullRebase) {
            if (src != null) {
                snapshots.remove(qsrc.getInstallPath());
            }
            qdest.setPrev(null);
            snapshots.put(qdest.getInstallPath(), qdest);
            return;
        }

        if (qsrc.getPrev() == null) {
            // base
            if (!qsrc.getNext().contains(qdest)) {
                qsrc.getNext().add(qdest);
                qdest.setPrev(qsrc);
            }
        } else {
            // intermediate
            Qcow2 toDelete = qdest.getPrev();
            while (toDelete != null) {
                if (qsrc.getNext().contains(toDelete)) {
                    qsrc.deleteNext(toDelete);
                    break;
                }
                toDelete = toDelete.getPrev();
            }

            qsrc.addNext(qdest);
            qdest.setPrev(qsrc);
        }

        dumpQcow2Tree(qdest, "Merging Snapshot");
    }

    public synchronized void delete(String installPath) {
        Qcow2 q = findByInstallPath(installPath);
        if (q == null) {
            return;
        }

        if (q.getPrev() == null) {
            // base
            snapshots.remove(q.getInstallPath());
        } else {
            // intermediate
            Qcow2 parent = q.getPrev();
            parent.deleteNext(q);
            dumpQcow2Tree(parent, "Deleting Snapshot");
        }
    }

    public synchronized RevertVolumeFromSnapshotResponse revert(RevertVolumeFromSnapshotCmd cmd) {
        RevertVolumeFromSnapshotResponse rsp = new RevertVolumeFromSnapshotResponse();
        Qcow2 current = findByInstallPath(cmd.getSnapshotInstallPath());
        if (current == null) {
            dumpAllQcow2();
            DebugUtils.Assert(false, String.format("cannot find source snapshot[%s]", cmd.getSnapshotInstallPath()));
        }
        String dir = PathUtil.parentFolder(cmd.getSnapshotInstallPath());
        String newVolumeInstallPath = String.format("%s/%s.qcow2", dir, Platform.getUuid());
        logger.debug(String.format("created new volume[%s] for reverting snapshot", newVolumeInstallPath));
        Qcow2.newQcow2(newVolumeInstallPath, current);

        dumpQcow2Tree(current, "Reverting Snapshot");

        rsp.setNewVolumeInstallPath(newVolumeInstallPath);
        return rsp;
    }

    private void walkDownLeaf(SnapshotLeaf leaf, List<VolumeSnapshotInventory> path, List<List<VolumeSnapshotInventory>> ret) {
        if (leaf.getChildren().isEmpty()) {
            List<VolumeSnapshotInventory> copy = new ArrayList<VolumeSnapshotInventory>();
            copy.addAll(path);
            copy.add(leaf.getInventory());
            ret.add(copy);
            return;
        }

        path.add(leaf.getInventory());
        for (SnapshotLeaf l : leaf.getChildren()) {
            walkDownLeaf(l, path, ret);
        }
        path.remove(leaf.getInventory());
    }

    private List<List<VolumeSnapshotInventory>> findOutAllChains(SnapshotLeaf leaf) {
        List<List<VolumeSnapshotInventory>> ret = new ArrayList<List<VolumeSnapshotInventory>>();
        List<VolumeSnapshotInventory> paths = new ArrayList<VolumeSnapshotInventory>();
        walkDownLeaf(leaf, paths, ret);
        return ret;
    }

    private void walkDownQcow2(Qcow2 qcow2, List<String> paths, List<List<String>> ret) {
        if (qcow2.getNext().isEmpty()) {
            List<String> copy = new ArrayList<String>();
            copy.addAll(paths);
            copy.add(qcow2.getInstallPath());
            ret.add(copy);
            return;
        }
        
        paths.add(qcow2.getInstallPath());
        for (Qcow2 q : qcow2.getNext()) {
            walkDownQcow2(q, paths, ret);
        }
        paths.remove(qcow2.getInstallPath());
    }
    
    private List<List<String>> findOutAllQcow2Chains(Qcow2 qcow2) {
        final List<String> paths = new ArrayList<String>();
        final List<List<String>> ret = new ArrayList<List<String>>();
        walkDownQcow2(qcow2, paths, ret);
        return ret;
    }
    
    private void validate(List<VolumeSnapshotInventory> chain) {
        VolumeSnapshotInventory start = chain.get(0);
        Qcow2 root = findByInstallPath(start.getPrimaryStorageInstallPath());
        if (root == null) {
            dumpAllQcow2();
            DebugUtils.Assert(false, String.format("cannot find root qcow2 with path[%s]", start.getPrimaryStorageInstallPath()));
        }

        List<List<String>> qcowChains = findOutAllQcow2Chains(root);

        List<String> expected = transformAndRemoveNull(chain, VolumeSnapshotInventory::getPrimaryStorageInstallPath);

        boolean success = false;
        for (List<String> paths : qcowChains) {
            boolean tempSuccess = true;
            if (paths.size() >= chain.size()) {
                for (VolumeSnapshotInventory s : chain) {
                    int index = chain.indexOf(s);
                    String actual = paths.get(index);
                    if (!actual.equals(s.getPrimaryStorageInstallPath())) {
                        tempSuccess = false;
                        break;
                    }
                }
            } else {
                tempSuccess = false;
            }

            if (tempSuccess) {
                success = true;
                break;
            }
        }

        if (!success) {
            StringBuilder sb = new StringBuilder("cannot find snapshot chain on backend:\n\n");
            sb.append(String.format("expected:\n\n"));
            sb.append(StringUtils.join(expected, "\n"));
            sb.append("\n\nactual:\n");
            for (List<String> paths : qcowChains) {
                sb.append(String.format("chain%s\n", qcowChains.indexOf(paths)));
                sb.append(StringUtils.join(paths, "\n"));
                sb.append("\n");
            }

            String err = sb.toString();
            logger.warn(err);
            DebugUtils.Assert(false, err);
        }
    }

    public void validateNotExisting(String installPath) {
        Qcow2 q = findByInstallPath(installPath);
        if (q != null) {
            DebugUtils.Assert(false, String.format("still found snapshot[%s]", q.getInstallPath()));
        }
    }


    public void validate(SnapshotLeaf leaf) {
        List<List<VolumeSnapshotInventory>> chains = findOutAllChains(leaf);
        for (List<VolumeSnapshotInventory> chain : chains) {
            validate(chain);
        }
    }

    public void validate(VolumeSnapshotInventory root) {
        logger.debug(String.format("validating volume snapshot chain starting with root[uuid:%s, installPath:%s]", root.getUuid(), root.getPrimaryStorageInstallPath()));
        SnapshotLeaf leaf = buildRootLeaf(root.getUuid());
        validate(leaf);
    }

    public SnapshotLeaf buildRootLeaf(String uuid) {
        VolumeSnapshotVO s = dbf.findByUuid(uuid, VolumeSnapshotVO.class);
        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.add(VolumeSnapshotVO_.treeUuid, Op.EQ, s.getTreeUuid());
        List<VolumeSnapshotVO> vos = q.list();
        VolumeSnapshotTree tree = VolumeSnapshotTree.fromVOs(vos);
        SnapshotLeaf leaf = tree.getRoot();
        if (!leaf.getInventory().getUuid().equals(uuid)) {
            DebugUtils.Assert(false, String.format("snapshot[%s] is not root snapshot", uuid));
        }
        return leaf;
    }
}
