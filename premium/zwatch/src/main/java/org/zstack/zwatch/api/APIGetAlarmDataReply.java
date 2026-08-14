package org.zstack.zwatch.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.datatype.AlarmData;

import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APIGetAlarmDataReply extends APIReply {
    private List<AlarmData> histories;

    private Long total;

    public List<AlarmData> getHistories() {
        return histories;
    }

    public void setHistories(List<AlarmData> histories) {
        this.histories = histories;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}
