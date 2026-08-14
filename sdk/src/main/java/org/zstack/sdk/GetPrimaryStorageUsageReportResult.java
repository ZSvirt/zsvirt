package org.zstack.sdk;

import org.zstack.sdk.UsageReport;

public class GetPrimaryStorageUsageReportResult {
    public java.util.Map uriUsageForecast;
    public void setUriUsageForecast(java.util.Map uriUsageForecast) {
        this.uriUsageForecast = uriUsageForecast;
    }
    public java.util.Map getUriUsageForecast() {
        return this.uriUsageForecast;
    }

    public UsageReport usageReport;
    public void setUsageReport(UsageReport usageReport) {
        this.usageReport = usageReport;
    }
    public UsageReport getUsageReport() {
        return this.usageReport;
    }

}
