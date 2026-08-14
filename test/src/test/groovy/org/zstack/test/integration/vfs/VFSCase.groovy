package org.zstack.test.integration.vfs

import org.zstack.testlib.SubCase
import org.zstack.testlib.vfs.Qcow2
import org.zstack.testlib.vfs.VFS
import org.zstack.testlib.vfs.tree.Qcow2Tree

import java.util.concurrent.TimeUnit

class VFSCase extends SubCase {
    VFS vfs

    @Override
    void clean() {
        if (vfs != null) {
            vfs.destroy()
        }
    }

    @Override
    void setup() {
    }

    @Override
    void environment() {
        vfs = new VFS()
    }

    void testGetFile() {
        Qcow2 qcow2 = vfs.createQcow2("/root/testGetFile.qcow2", 100L)
        Qcow2 q1 = vfs.getFile("/root/testGetFile.qcow2")
        assert q1.exists()
        assert qcow2 == q1
    }

    void testSnapshot() {
        Qcow2 q1 = vfs.createQcow2("/root/testSnapshot.qcow2", 100L)

        Qcow2 snapshot = q1.snapshot("/root/testSnapshot-snapshot.qcow2")
        assert snapshot.exists()

        Qcow2 sp1 = vfs.getFile("/root/testSnapshot-snapshot.qcow2")
        assert sp1.backingFile == q1.path

        sp1.assertQcow2Compare(sp1)

        sp1.delete()
        sp1 = vfs.getFile("/root/testSnapshot-snapshot.qcow2")
        assert sp1 == null
    }

    void testFindFile() {
        Qcow2 qcow2 = vfs.createQcow2("/root/testFindFile.qcow2", 100L)
        assert qcow2.exists()

        Qcow2 q2 = vfs.findFile {
            return it.path == qcow2.path
        }

        assert q2 == qcow2
    }

    void testVFSQcow2Tree() {
        Qcow2 threeSnapshotQcow2 = vfs.createQcow2("/root/test/threeSnapshotQcow2.qcow2", 100L)
        Qcow2 ssp1 = threeSnapshotQcow2.snapshot("/root/test/snapshots/sp1.qcow2")
        Qcow2 ssp2 = ssp1.snapshot("/root/test/snapshots/sp2.qcow2")
        Qcow2 ssp3 = ssp2.snapshot("/root/test/snapshots/sp3.qcow2")

        assert ssp3 == vfs.findFile { return it.path == ssp3.path }

        assert null == vfs.findFile { return it.path.toAbsolutePath().toString() == "abcd" }

        Qcow2Tree tree = vfs.vfsQcow2Tree()
        assert !tree.rootNodes.isEmpty()

        Qcow2Tree.Qcow2Node rootNode = tree.rootNodes[threeSnapshotQcow2.pathString()]
        assert rootNode != null

        assert rootNode.findNode { us, parent ->
            return us.qcow2 == threeSnapshotQcow2 && parent == null
        }

        assert rootNode.findNode { us, parent ->
            return us.qcow2 == ssp1 && parent != null && parent.qcow2 == threeSnapshotQcow2
        }

        assert rootNode.findNode { us, parent ->
            return us.qcow2 == ssp2 && parent != null && parent.qcow2 == ssp1
        }

        assert rootNode.findNode { us, parent ->
            return us.qcow2 == ssp3 && parent != null && parent.qcow2 == ssp2
        }
    }

    void testCreate() {
        Qcow2 qcow2 = vfs.createQcow2("/root/test.qcow2", 100L)
        assert qcow2.exists()
    }

    void testCopy() {
        Qcow2 qcow2 = vfs.createQcow2("/root/testCopy.qcow2", 100L)

        expect(Exception.class) {
            qcow2.copy(qcow2.pathString())
        }

        Qcow2 copy = qcow2.copy("/root/testCopy-1.qcow2")

        assert qcow2.compare(copy)
    }

    void testMove() {
        Qcow2 qcow2 = vfs.createQcow2("/root/testMove.qcow2", 100L)
        Qcow2 sp = qcow2.snapshot("/root/testMove-snapshot.qcow2",)

        expect(Exception.class) {
            sp.move(sp.pathString())
        }

        Qcow2 move = sp.move("/root/testMove-1.qcow2")
        assert move.compare(sp)
        assert vfs.getFile(sp.pathString()) == null
    }

    void testDelete() {
        Qcow2 q1 = vfs.createQcow2("/root/testDelete/1.qcow2", 100L)
        Qcow2 q2 = vfs.createQcow2("/root/testDelete/2.qcow2", 100L)
        vfs.delete(q1.path)
        assert vfs.getFile(q1.pathString()) == null
        vfs.delete(q2.path)
        assert vfs.getFile(q2.pathString()) == null

        Set<String> files = []
        vfs.walkFileSystem { f ->
            files.add(f.pathString())
        }

        assert !files.contains(q1.pathString()) : "${q1.pathString()} still found by walkFileSystem"
        assert !files.contains(q2.pathString()) : "${q2.pathString()} still found by walkFileSystem"
    }

    @Override
    void test() {
        testCreate()
        testGetFile()
        testSnapshot()
        testFindFile()
        testVFSQcow2Tree()
        testCopy()
        testMove()
        testDelete()
    }
}
