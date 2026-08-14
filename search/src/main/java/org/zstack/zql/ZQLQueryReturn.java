package org.zstack.zql;

import org.zstack.header.log.NoLogging;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ZQLQueryReturn implements Serializable {
    @NoLogging(behavior = NoLogging.Behavior.Auto)
    public List inventories;
    public LinkedHashMap<Object, Long> inventoryCounts;
    public Long total;
    public Map returnWith;
    public String name;
}
