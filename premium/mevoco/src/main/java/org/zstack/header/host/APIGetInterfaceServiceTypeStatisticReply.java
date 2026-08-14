package org.zstack.header.host;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APIGetInterfaceServiceTypeStatisticReply extends APIReply {
    private List<ServiceTypeStatisticData> serviceTypeStatistics;

    private Long total;

    public List<ServiceTypeStatisticData> getServiceTypeStatistics() {
        return serviceTypeStatistics;
    }

    public void setServiceTypeStatistics(List<ServiceTypeStatisticData> serviceTypeStatistics) {
        this.serviceTypeStatistics = serviceTypeStatistics;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }


    public static APIGetInterfaceServiceTypeStatisticReply __example__() {
        APIGetInterfaceServiceTypeStatisticReply reply = new APIGetInterfaceServiceTypeStatisticReply();
        ServiceTypeStatisticData data = new ServiceTypeStatisticData();
        data.setInterfaceUuid(uuid());
        data.setServiceTypes(Collections.singletonList(ServiceTypeStatisticConstants.ServiceType.MANAGEMENT_NETWORK));
        reply.setServiceTypeStatistics(Collections.singletonList(data));
        reply.setTotal(1L);
        return reply;
    }
}
