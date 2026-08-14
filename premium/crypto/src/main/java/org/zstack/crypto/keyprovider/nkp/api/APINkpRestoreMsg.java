package org.zstack.crypto.keyprovider.nkp.api;

import org.zstack.header.keyprovider.NkpRestoreInfo;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.GsonTransient;
import org.zstack.header.rest.APINoSee;

public abstract class APINkpRestoreMsg extends APIMessage {
    @APIParam(emptyString = false)
    @NoLogging
    private String contentBase64;

    @APIParam(required = false, maxLength = 255)
    @NoLogging
    private String password;

    @APINoSee
    @GsonTransient
    private NkpRestoreInfo restoreInfo;

    @APINoSee
    @GsonTransient
    private String parseCode;

    @APINoSee
    @GsonTransient
    private String parseReason;

    public String getContentBase64() {
        return contentBase64;
    }

    public void setContentBase64(String contentBase64) {
        this.contentBase64 = contentBase64;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public NkpRestoreInfo getRestoreInfo() {
        return restoreInfo;
    }

    public void setRestoreInfo(NkpRestoreInfo restoreInfo) {
        this.restoreInfo = restoreInfo;
    }

    public String getParseCode() {
        return parseCode;
    }

    public void setParseCode(String parseCode) {
        this.parseCode = parseCode;
    }

    public String getParseReason() {
        return parseReason;
    }

    public void setParseReason(String parseReason) {
        this.parseReason = parseReason;
    }
}
