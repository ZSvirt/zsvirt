package org.zstack.zwatch.datatype;

import java.util.function.Predicate;

public interface HostNetworkMetricFilter extends Predicate {
    String getFilterName();
}
