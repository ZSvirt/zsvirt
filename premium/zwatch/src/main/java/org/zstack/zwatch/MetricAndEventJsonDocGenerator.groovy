package org.zstack.zwatch

import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.zwatch.datatype.Namespace

class MetricAndEventJsonDocGenerator {
    static class Doc {
        String namespace
        String name
        List<String> labelNames
    }

    private List<Doc> docs = []

    MetricAndEventJsonDocGenerator() {
        Namespace.namespaces.values().each { ns ->
            if (ns.getMetrics() != null) {
                ns.getMetrics().each { m ->
                    docs.add(new Doc(namespace: ns.getName(), name: m.name, labelNames:  m.labelNames))
                }
            }

            if (ns.getEvents() != null) {
                ns.getEvents().each { e ->
                    docs.add(new Doc(namespace: ns.getName(), name: e.name, labelNames: e.labelNames))
                }
            }
        }
    }

    @Override
    String toString() {
        return JSONObjectUtil.toJsonString(docs)
    }
}
