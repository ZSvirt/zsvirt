package org.zstack.zql;

public class SlowZQLStatistic {
    private long cost;
    private String zql;

    public SlowZQLStatistic(String zql, long cost) {
        this.zql = zql;
        this.cost = cost;
    }

    public long getCost() {
        return cost;
    }

    public void setCost(long cost) {
        this.cost = cost;
    }

    public String getZql() {
        return zql;
    }

    public void setZql(String zql) {
        this.zql = zql;
    }
}
