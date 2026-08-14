package org.zstack.zwatch.api;

import org.zstack.header.message.MessageReply;
import org.zstack.zwatch.datatype.Datapoint;

import java.util.List;

/**
 * Created by lining on 2018/10/17.
 */
public class GetMetricDataReply extends MessageReply {
    private List<Datapoint> datas;

    public List<Datapoint> getDatas() {
        return datas;
    }

    public void setDatas(List<Datapoint> datas) {
        this.datas = datas;
    }
}
