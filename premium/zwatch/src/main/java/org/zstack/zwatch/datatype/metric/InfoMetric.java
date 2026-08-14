package org.zstack.zwatch.datatype.metric;

import java.util.Collection;

/**
 * InfoMetric marks metrics that represent basic resource info labels.
 */
public class InfoMetric extends CountMetric {
    public InfoMetric(String name, Collection collection, Enum... labelNames) {
        super(name, collection, labelNames);
    }

    public InfoMetric(String name, Collection collection, boolean adminOnly, Enum... labelNames) {
        super(name, collection, adminOnly, labelNames);
    }
}
