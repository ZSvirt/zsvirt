package org.zstack.testlib.vfs.tree

import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import java.util.function.Consumer
import java.util.function.Function

class PathNode {
    private static CLogger logger = Utils.getLogger(PathNode.class)

    // the node purely organized by filesystem path
    String path
    PathNode parent
    List<PathNode> children = []

    private void doPrint(PrintWriter writer, String prefix, boolean isTail) {
        children.sort(new Comparator<PathNode>() {
            @Override
            int compare(PathNode o1, PathNode o2) {
                return o1.path <=> o2.path
            }
        })

        writer.println(prefix + (isTail ? "|__ " : "|---") + path)
        for (int i=0; i<children.size()-1; i++) {
            children.get(i).doPrint(writer, prefix + (isTail ? "    " : "|   "), false)
        }

        if (children.size() >= 1) {
            children.get(children.size()-1).doPrint(writer, prefix + (isTail ? "    " : "|   "), true)
        }
    }

    String dumpAsString(boolean banner = true) {
        StringWriter stringWriter = new StringWriter()
        PrintWriter printWriter = new PrintWriter(stringWriter)

        if (banner) {
            printWriter.println("==================== DUMP PathNode ${path} =======================")
        }

        doPrint(printWriter, "", true)

        if (banner) {
            printWriter.println("===================== End PathNode Dump ==========================")
        }
        //logger.debug("\n\n${stringWriter.toString()}")
        return stringWriter.toString()
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (!(o instanceof PathNode)) return false

        PathNode pathNode = (PathNode) o

        if (path != pathNode.path) return false

        return true
    }

    int hashCode() {
        return (path != null ? path.hashCode() : 0)
    }

    static enum PathNodeDifference {
        DifferentParent,
        DifferentChildren,
        DifferentPath
    }

    static class PathNodeComparisonResult {
        String leftNodePath
        String rightNodePath
        PathNodeDifference difference
        String comment


        @Override
        String toString() {
            return "PathNodeComparisonResult{" +
                    "leftNodePath='" + leftNodePath + '\'' +
                    ", rightNodePath='" + rightNodePath + '\'' +
                    ", difference=" + difference +
                    ", comment='" + comment + '\'' +
                    '}'
        }
    }

    private void doWalk(PathNode node, Consumer<PathNode> consumer) {
        consumer.accept(node)
        node.children.each { doWalk(it, consumer) }
    }

    void walk(Consumer<PathNode> consumer) {
        doWalk(this, consumer)
    }

    private PathNode doFind(PathNode node, Function<PathNode, Boolean> func) {
        if (func.apply(node)) {
            return node
        }

        for (PathNode n : node.children) {
            PathNode fn = doFind(n, func)
            if (fn != null) {
                return fn
            }
        }

        return null
    }

    PathNode findNode(Function<PathNode, Boolean> func) {
        return doFind(this, func)
    }

    static PathNodeComparisonResult compare(PathNode left, PathNode right) {
        if (left.path != right.path) {
            return new PathNodeComparisonResult(
                    leftNodePath: left.path,
                    rightNodePath: right.path,
                    difference: PathNodeDifference.DifferentPath,
                    comment: "compared with a different qcow2[${right.path}]"
            )
        }

        if (left?.parent?.path != right?.parent?.path) {
            return new PathNodeComparisonResult(
                    leftNodePath: left.path,
                    rightNodePath: right.path,
                    difference: PathNodeDifference.DifferentParent,
                    comment: "left node parent[${left?.parent?.path}], right node parent[${right?.parent?.path}]"
            )
        }

        List<String> leftNodeChildrenPaths = left.children.collect { it.path }.sort()
        List<String> rightNodeChildrenPaths = right.children.collect { it.path }.sort()
        if (leftNodeChildrenPaths != rightNodeChildrenPaths) {
            return new PathNodeComparisonResult(
                    leftNodePath: left.path,
                    rightNodePath: right.path,
                    difference: PathNodeDifference.DifferentChildren,
                    comment: "left node children: \n${leftNodeChildrenPaths}\nright node children:\n${rightNodeChildrenPaths}"
            )
        }

        left.children.sort(new Comparator<PathNode>() {
            @Override
            int compare(PathNode o1, PathNode o2) {
                return o1.path <=> o2.path
            }
        })

        right.children.sort(new Comparator<PathNode>() {
            @Override
            int compare(PathNode o1, PathNode o2) {
                return o1.path <=> o2.path
            }
        })

        for (int i=0; i<left.children.size(); i++) {
            PathNode lchild = left.children[i]
            PathNode rchild = right.children[i]
            assert lchild.path == rchild.path

            PathNodeComparisonResult r = compare(lchild, rchild)
            if (r != null) {
                return r
            }
        }

        return null
    }

    PathNodeComparisonResult compare(PathNode right) {
        return compare(this, right)
    }

    static List<PathNode> fromQcow2Tree(Qcow2Tree tree) {
        List<PathNode> nodes = []

        tree.rootNodes.values().each {
            Map<String, PathNode> tmp = [:]
            it.walk { us, parent ->
                PathNode me = tmp.computeIfAbsent(us.path(), { new PathNode(path: us.path())})
                if (parent != null) {
                    PathNode p = tmp.computeIfAbsent(parent.path(), { new PathNode(path: parent.path()) })
                    if (!p.children.contains(me)) {
                        p.children.add(me)
                    }

                    assert me.parent == null || me.parent == p : "qcow2 node[${me.path}] already has a parent[${me.parent.path}]," +
                            "but a new parent[${p.path}] found"

                    me.parent = p
                }
            }

            List<PathNode> n = tmp.values().findAll { it.parent == null }
            assert !n.isEmpty() : "no root PathNode whose parent is null found for Qcow2Tree[${tree}]"
            assert n.size() == 1 : "more than one root PathNode whose parent is null found for Qcow2Tree[${tree}]"
            nodes.add(n[0])
        }

        return nodes
    }
}
