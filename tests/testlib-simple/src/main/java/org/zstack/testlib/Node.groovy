package org.zstack.testlib

import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger
import org.zstack.utils.tester.ZTester

/**
 * Created by xing5 on 2017/2/12.
 */
trait Node {
    final CLogger logger = Utils.getLogger(this.getClass())
    final ZTester testter = Utils.getTester()

    Node parent
    List<Object> children = []
    Set<Node> dependencies = []

    void accept(NodeVisitor v) {
        v.visit(this)
    }

    void addChild(Object child) {
        if (child instanceof Node) {
            child.parent = this
            child.dependencies.add(this)
        }

        children.add(child)
    }

    void walkNode(Object n, Closure c) {
        if (n instanceof Node) {
            c(n)

            n.children.each {
                walkNode(it, c)
            }
        } else if (n instanceof ActionNode) {
            n.run()
        } else {
            assert true: "unknown node type ${n.class}"
        }
    }

    void walk(Closure c) {
        walkNode(this, c)
    }

    def find(Node n, String name, Class type) {
        if (type.isAssignableFrom(n.class) && n.name == name) {
            return n
        }

        for (Object c : n.children) {
            if (c instanceof Node) {
                def ret = find(c, name, type)
                if (ret != null) {
                    return ret
                }
            }
        }

        return null
    }

    def find(String name, Class type) {
        return find(this, name, type)
    }

    void resolveDependency(Node n, Set<Node> resolved, List<Node> seen) {
        seen.push(n)
        n.dependencies.each {
            if (!(it instanceof Node)) {
                return
            }

            if (it in seen) {
                seen.push(it)
                def names = seen.collect { sn ->
                    return sn.hasProperty("name") ? sn.name : sn.toString()
                }

                throw new Exception("circle dependencies in environment(): ${names.join(" --> ")}")
            }

            resolveDependency(it, resolved, seen)
        }

        resolved.add(n)
        seen.pop()
    }

    void destroy(String sessionId) {
        def allNodes = []

        walk {
            allNodes.add(it)
        }

        LinkedHashSet<Node> resolvedNodes = new LinkedHashSet<>()
        allNodes.each {
            resolveDependency(it as Node, resolvedNodes, [])
        }

        def reversedNodes = resolvedNodes.toList()
        Collections.reverse(reversedNodes)
        reversedNodes.each { Node n ->
            if (n instanceof DeleteAction) {
                logger.debug("destroy on " + n.toString())
                n.delete(sessionId)
            }
        }
    }
}
