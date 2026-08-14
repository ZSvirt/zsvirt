package org.zstack.header.search;

import org.zstack.header.message.APIEvent;

public class APISearchGenerateSqlTriggerEvent extends APIEvent {
    private String resultPath;

    public String getResultPath() {
        return resultPath;
    }

    public void setResultPath(String resultPath) {
        this.resultPath = resultPath;
    }

    public APISearchGenerateSqlTriggerEvent(String id) {
        super(id);
    }

    public APISearchGenerateSqlTriggerEvent() {
        super(null);
    }
 
    public static APISearchGenerateSqlTriggerEvent __example__() {
        APISearchGenerateSqlTriggerEvent event = new APISearchGenerateSqlTriggerEvent();


        return event;
    }

}
