package org.zstack.testlib.vfs.tree

import org.zstack.testlib.vfs.Qcow2
import org.zstack.testlib.vfs.VFS

import java.util.function.BiConsumer
import java.util.function.BiFunction

class Qcow2Tree {
    protected transient VFS vfs

    static class Qcow2Node {
        Qcow2 qcow2
        Qcow2Node parent
        List<Qcow2Node> children

        String path() {
            return qcow2.pathString()
        }

        private void doWalk(Qcow2Node us, BiConsumer<Qcow2Node, Qcow2Node> consumer) {
            consumer.accept(us, us.parent)

            us.children.each {
                doWalk(it, consumer)
            }
        }

        void walk(BiConsumer<Qcow2Node, Qcow2Node> consumer) {
            doWalk(this, consumer)
        }

        private Qcow2Node doFindNode(Qcow2Node us, BiFunction<Qcow2Node, Qcow2Node, Boolean> func) {
            if (func.apply(us, us.parent)) {
                return us
            }

            if (us.children.isEmpty()) {
                return null
            }

            for (Qcow2Node child : us.children) {
                return doFindNode(child, func)
            }
        }

        Qcow2Node findNode(BiFunction<Qcow2Node, Qcow2Node, Boolean> func) {
            return doFindNode(this, func)
        }
    }

    Map<String, Qcow2Node> rootNodes = [:]

    String dumpAsString() {
        return PathNode.fromQcow2Tree(this).collect { it.dumpAsString() }.join("\n")
    }

    Qcow2Tree(VFS vfs1) {
        this.vfs = vfs1

        Map<String, Qcow2Node> allNodes = [:]

        vfs.walkFileSystem { vf ->
            if (!(vf instanceof Qcow2)) {
                return
            }

            Qcow2 f = (Qcow2) vf
            Qcow2Node n = allNodes[f.pathString()]
            if (n == null) {
                n = new Qcow2Node(qcow2: f, children: [])
                allNodes[f.pathString()] = n
            }

            if (f.backingFile != null) {
                Qcow2 p = vfs.getFile(f.backingFile.toAbsolutePath().toString())
                assert p : "cannot find file[${f.pathString()}]'s backing file[${f.backingFile.toAbsolutePath().toString()}]" +
                        " on VFS[id: ${vfs.id}]. Dump of whole VFS:\n${vfs.dumpAsString()}"

                Qcow2Node pnode  = allNodes[p.pathString()]
                if (pnode == null) {
                    pnode = new Qcow2Node(qcow2: p, children: [])
                    allNodes[p.pathString()] = pnode
                }

                pnode.children.add(n)
                n.parent = pnode
            }
        }

        allNodes.each { p, n ->
            assert !n.children.contains(n) : "${n.qcow2} contains itself as a child"

            if (n.parent == null) {
                rootNodes[p] = n
            }
        }
    }
}
