package org.zstack.crypto.keyprovider.nkp.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"content"})
public class APIBackupNkpEvent extends APIEvent {
    private String content;

    public APIBackupNkpEvent() {
        super(null);
    }

    public APIBackupNkpEvent(String apiId) {
        super(apiId);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public static APIBackupNkpEvent __example__() {
        APIBackupNkpEvent event = new APIBackupNkpEvent();
        event.setContent("BASE64_ENCODED_NKP_BACKUP");
        return event;
    }
}
