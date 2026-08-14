package org.zstack.core.errorcode;

import org.zstack.header.rest.SDK;

import java.io.Serializable;

/**
 * Created by mingjian.deng on 2018/12/21.
 */
@SDK
public class ElaborationCheckResult implements Serializable {
    private String fileName;
    private String content;
    private String reason;

    public ElaborationCheckResult() {
    }

    public ElaborationCheckResult(String fileName, String content, String reason) {
        this.fileName = fileName;
        this.content = content;
        this.reason = reason;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
