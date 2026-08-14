package org.zstack.billing;

/**
 * @author: kefeng.wang
 * @date: 2019-03-05
 **/
public class Pagination {
    private Integer start = 0;
    private Integer limit = 0;
    private Integer total = 0;

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        if (start != null) {
            this.start = start;
        }
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        if (limit != null) {
            this.limit = limit;
        }
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        if (total != null) {
            this.total = total;
        }
    }
}
