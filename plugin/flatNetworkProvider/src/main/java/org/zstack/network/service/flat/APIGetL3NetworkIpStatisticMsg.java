package org.zstack.network.service.flat;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.network.l3.L3NetworkMessage;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;

import static org.zstack.network.service.flat.IpStatisticConstants.*;

/**
 * Created by Qi Le on 2019/9/9
 */
@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/ip-statistic",
        method = HttpMethod.GET,
        responseClass = APIGetL3NetworkIpStatisticReply.class
)
public class APIGetL3NetworkIpStatisticMsg extends APISyncCallMessage implements L3NetworkMessage {
    @APIParam(resourceType = L3NetworkVO.class)
    private String l3NetworkUuid;

    @APIParam(validValues = {ResourceType.ALL, ResourceType.VIP, ResourceType.VM, ResourceType.KERNEL_INTERFACE}, required = false)
    private String resourceType = ResourceType.ALL;

    @APIParam(required = false)
    private String ip;

    @APIParam(validValues = {SortBy.IP, SortBy.CREATE_TIME}, required = false)
    private String sortBy = SortBy.IP;

    @APIParam(validValues = {SortDirection.ASC, SortDirection.DESC}, required = false)
    private String sortDirection = SortDirection.ASC;

    @APIParam(numberRange = {0, Integer.MAX_VALUE}, required = false)
    private Integer start = 0;

    @APIParam(numberRange = {0, Integer.MAX_VALUE}, required = false)
    private Integer limit = 20;

    @APIParam(required = false)
    private boolean replyWithCount;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public boolean isReplyWithCount() {
        return replyWithCount;
    }

    public void setReplyWithCount(boolean replyWithCount) {
        this.replyWithCount = replyWithCount;
    }

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public static APIGetL3NetworkIpStatisticMsg __example__() {
        APIGetL3NetworkIpStatisticMsg msg = new APIGetL3NetworkIpStatisticMsg();
        msg.setL3NetworkUuid(uuid());
        return msg;
    }
}
