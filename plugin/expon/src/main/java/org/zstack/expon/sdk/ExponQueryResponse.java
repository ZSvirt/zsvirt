package org.zstack.expon.sdk;

public class ExponQueryResponse extends ExponResponse {
    protected long total;
    protected long count;
    public void setTotal(long total) {
        this.total = total;
    }

    public long getTotal() {
        return total;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getCount() {
        return count;
    }
}
