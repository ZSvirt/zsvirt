package org.zstack.zsv.telemetry.header;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;

public class TelemetrySyncReportMsg extends NeedReplyMessage {
    private String snapshotDate;
    private String fileName;

    @NoLogging(type = NoLogging.Type.LongText)
    private String reportJson;

    public String getSnapshotDate() {
        return snapshotDate;
    }

    public void setSnapshotDate(String snapshotDate) {
        this.snapshotDate = snapshotDate;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getReportJson() {
        return reportJson;
    }

    public void setReportJson(String reportJson) {
        this.reportJson = reportJson;
    }
}
