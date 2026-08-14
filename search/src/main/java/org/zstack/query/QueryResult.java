package org.zstack.query;

import java.util.List;

/**
 */
public class QueryResult {
    private List inventories;
    private Long total;

    public List getInventories() {
        return inventories;
    }

    public void setInventories(List inventories) {
        this.inventories = inventories;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}
