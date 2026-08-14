package org.zstack.zwatch.alarm.sns.template.aliyunsms;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by Qi Le on 2019-07-15
 */
@RestResponse(allTo = "inventories")
public class APIQueryAliyunSmsSNSTextTemplateReply extends APIQueryReply {
    private List<AliyunSmsSNSTextTemplateInventory> inventories;

    public List<AliyunSmsSNSTextTemplateInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AliyunSmsSNSTextTemplateInventory> inventories) {
        this.inventories = inventories;
    }
}
