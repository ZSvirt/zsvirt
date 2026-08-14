package org.zstack.simulator2.config

/**
 * Created by xing5 on 2017/9/17.
 */
trait JsonNode {
    transient JsonNode parent
    transient List<JsonNode> children = []
    boolean isAssembled

    void delete() {
        if (parent == null) {
            return
        }

        parent.children.remove(this)
        parent = null
    }

    void add(JsonNode node) {
        node.parent = this
        children.add(node)
    }

    void assemble(JsonNode parent) {
        if (parent.isAssembled) {
            return
        }

        parent.isAssembled = true

        parent.getProperties().each { name, value ->
            if (name == "parent" || name == "children") {
                return
            }

            if (value instanceof JsonNode) {
                assemble(value)

                value.parent = parent
                parent.children.add(value)
            } else if (value instanceof Collection) {
                value.each { n ->
                    if (n instanceof JsonNode) {
                        assemble(n)

                        n.parent = parent
                        parent.children.add(n)
                    }
                }
            } else if (value instanceof Map) {
                value.values().each { n ->
                    if (n instanceof JsonNode) {
                        assemble(n)

                        n.parent = parent
                        parent.children.add(n)
                    }
                }
            }
        }
    }

    void assemble() {
        assemble(this)
    }

    def <T> T find(Closure c) {
        boolean ret = c(this)
        if (ret) {
            return this as T
        }

        for (JsonNode child : children) {
            ret = child.find(c)
            if (ret) {
                return child as T
            }
        }

        return null
    }

    boolean visit(Closure c) {
        if (c(this) == true) {
            return true
        }

        for (JsonNode child : children) {
            if (child.visit(c)) {
                return true
            }
        }

        return false
    }
}
