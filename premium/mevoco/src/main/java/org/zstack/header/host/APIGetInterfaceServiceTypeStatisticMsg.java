package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static org.zstack.header.host.ServiceTypeStatisticConstants.*;

@RestRequest(
        path = "/hosts/hosts-network-interfaces/service-type-statistic",
        responseClass = APIGetInterfaceServiceTypeStatisticReply.class,
        method = HttpMethod.GET
)
public class APIGetInterfaceServiceTypeStatisticMsg extends APISyncCallMessage {
    @APIParam(required = false)
    private String interfaceUuid;

    @APIParam(required = false)
    private Integer vlanId;

    @APIParam(validValues = {InterfaceType.ALL, InterfaceType.INTERFACE, InterfaceType.BONDING}, required = false)
    private String interfaceType = InterfaceType.ALL;

    @APIParam(required = false)
    private List<String> serviceType;

    @APIParam(required = false)
    private String zoneUuid;

    @APIParam(required = false)
    private String clusterUuid;

    @APIParam(required = false)
    private String hostUuid;

    @APIParam(validValues = {SortBy.INTERFACE_NAME, SortBy.VLAN_ID, SortBy.HOST_IP, SortBy.HOST_NAME, SortBy.CLUSTER_NAME, SortBy.CREATE_DATE}, required = false)
    private String sortBy = SortBy.CREATE_DATE;

    @APIParam(validValues = {SortDirection.ASC, SortDirection.DESC}, required = false)
    private String sortDirection = SortDirection.ASC;

    @APIParam(numberRange = {0, Integer.MAX_VALUE}, required = false)
    private Integer start = 0;

    @APIParam(numberRange = {0, Integer.MAX_VALUE}, required = false)
    private Integer limit = 20;

    @APIParam(required = false)
    private boolean replyWithCount;

    public String getInterfaceUuid() {
        return interfaceUuid;
    }

    public void setInterfaceUuid(String interfaceUuid) {
        this.interfaceUuid = interfaceUuid;
    }

    public Integer getVlanId() {
        return vlanId;
    }

    public void setVlanId(Integer vlanId) {
        this.vlanId = vlanId;
    }

    public String getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(String interfaceType) {
        this.interfaceType = interfaceType;
    }

    public List<String> getServiceType() {
        return serviceType;
    }

    public void setServiceType(List<String> serviceType) {
        this.serviceType = serviceType;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
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

    public static APIGetInterfaceServiceTypeStatisticMsg __example__() {
        APIGetInterfaceServiceTypeStatisticMsg msg = new APIGetInterfaceServiceTypeStatisticMsg();
        msg.setInterfaceUuid(uuid());
        return msg;
    }
}
