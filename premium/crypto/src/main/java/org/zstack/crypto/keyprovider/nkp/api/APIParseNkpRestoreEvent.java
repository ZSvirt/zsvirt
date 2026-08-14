package org.zstack.crypto.keyprovider.nkp.api;

import org.zstack.header.keyprovider.KeyProviderErrors;
import org.zstack.header.keyprovider.NkpRestoreInfo;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APIParseNkpRestoreEvent extends APIEvent {
    private NkpRestoreInfo restoreInfo;
    private String code;
    private String reason;

    public APIParseNkpRestoreEvent() {
        super(null);
    }

    public APIParseNkpRestoreEvent(String apiId) {
        super(apiId);
    }

    public NkpRestoreInfo getRestoreInfo() {
        return restoreInfo;
    }

    public void setRestoreInfo(NkpRestoreInfo restoreInfo) {
        this.restoreInfo = restoreInfo;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public static APIParseNkpRestoreEvent __example__() {
        APIParseNkpRestoreEvent event = new APIParseNkpRestoreEvent();
        event.setCode(KeyProviderErrors.OK.toString());
        event.setRestoreInfo(NkpRestoreInfo.__example__());
        return event;
    }
}
