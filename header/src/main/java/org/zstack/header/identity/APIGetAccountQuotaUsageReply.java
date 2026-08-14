package org.zstack.header.identity;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 2/22/2016.
 */
@RestResponse(allTo = "usages")
public class APIGetAccountQuotaUsageReply extends APIReply {
    private List<Quota.QuotaUsage> usages;

    public List<Quota.QuotaUsage> getUsages() {
        return usages;
    }

    public void setUsages(List<Quota.QuotaUsage> usages) {
        this.usages = usages;
    }
 
    public static APIGetAccountQuotaUsageReply __example__() {
        APIGetAccountQuotaUsageReply reply = new APIGetAccountQuotaUsageReply();
        Quota.QuotaUsage usage = new Quota.QuotaUsage();
        usage.setName("testquota");
        usage.setTotal(20L);
        usage.setUsed(10L);
        reply.setUsages(list(usage));
        return reply;
    }

}
