package org.zstack.zwatch.thirdparty.xsky;

import org.zstack.zwatch.thirdparty.GetAlertsRsp;

import java.util.List;
import java.util.Map;

/**
 * Created by yaoning.li on 2020/7/16.
 */
abstract class GetXSKYAlertsBaseRsp implements GetAlertsRsp {

    public abstract List<Map> getAlerts();


    protected Paging paging;

    public Paging getPaging() {
        return paging;
    }

    public void setPaging(Paging paging) {
        this.paging = paging;
    }
}
public class GetXSKYAlertsRsp extends GetXSKYAlertsBaseRsp {
    private List<Map> alerts;

    @Override
    public List<Map> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<Map> alerts) {
        this.alerts = alerts;
    }

}

class Paging {
    private long count;
    private long limit;
    private long offset;
    private long total_count;

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getLimit() {
        return limit;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getTotal_count() {
        return total_count;
    }

    public void setTotal_count(long total_count) {
        this.total_count = total_count;
    }
}