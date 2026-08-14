package org.zstack.header.storage.primary;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Map;

@RestResponse(fieldsTo = {"all"})
public class APIGetPrimaryStorageUsageReportReply extends APIReply {
    private Map<String, UsageReport> uriUsageForecast;
    private UsageReport usageReport;

    public Map<String, UsageReport> getUriUsageForecast() {
        return uriUsageForecast;
    }

    public void setUriUsageForecast(Map<String, UsageReport> uriUsageForecast) {
        this.uriUsageForecast = uriUsageForecast;
    }

    public UsageReport getUsageReport() {
        return usageReport;
    }

    public void setUsageReport(UsageReport usageReport) {
        this.usageReport = usageReport;
    }

    public static APIGetPrimaryStorageUsageReportReply __example__() {
        APIGetPrimaryStorageUsageReportReply reply = new APIGetPrimaryStorageUsageReportReply();

        reply.setUsageReport(new UsageReport());
        reply.setUriUsageForecast(new java.util.HashMap<String, UsageReport>());

        return reply;
    }
}
