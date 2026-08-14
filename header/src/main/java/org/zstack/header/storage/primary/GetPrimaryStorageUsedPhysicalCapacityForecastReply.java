package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

import java.util.Map;

public class GetPrimaryStorageUsedPhysicalCapacityForecastReply extends MessageReply {
    private Map<String, UsageReport> usageReportMap;

    public Map<String, UsageReport> getUsageReportMap() {
        return usageReportMap;
    }

    public void setUsageReportMap(Map<String, UsageReport> usageReportMap) {
        this.usageReportMap = usageReportMap;
    }
}
