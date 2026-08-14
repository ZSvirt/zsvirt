package org.zstack.billing;

import org.zstack.header.message.APIReply;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by kefeng.wang on 12/27/2018.
 */
@RestResponse(fieldsTo = {"all"})
public class APICalculateResourceSpendingReply extends APIReply {
    private List<ResourceSpending> spending;

    private Pagination pagination;

    public List<ResourceSpending> getSpending() {
        return spending;
    }

    public void setSpending(List<ResourceSpending> spending) {
        this.spending = spending;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    public static APICalculateResourceSpendingReply __example__() {
        ResourceSpending spending = new ResourceSpending();
        spending.setResourceType(BillingConstants.SPENDING_TYPE_VM);
        spending.setResourceUuid("92862bcdc424479cbcc9319ded346174");
        spending.setResourceName("VM-CMP");
        spending.setStartTime(DocUtils.date);
        spending.setEndTime(DocUtils.date);
        spending.setSpending(123456.78);

        APICalculateResourceSpendingReply reply = new APICalculateResourceSpendingReply();
        reply.setSpending(list(spending));
        return reply;
    }
}
