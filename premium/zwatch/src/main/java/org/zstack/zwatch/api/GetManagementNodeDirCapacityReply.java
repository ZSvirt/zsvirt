package org.zstack.zwatch.api;

import org.zstack.header.message.MessageReply;
import org.zstack.zwatch.datatype.CapacityData;

import java.util.List;

public class GetManagementNodeDirCapacityReply extends MessageReply {
    private List<CapacityData> dataList;

    public List<CapacityData> getDataList() {
        return dataList;
    }

    public void setDataList(List<CapacityData> dataList) {
        this.dataList = dataList;
    }
}
