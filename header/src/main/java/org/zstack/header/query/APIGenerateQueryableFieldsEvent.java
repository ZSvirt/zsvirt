package org.zstack.header.query;

import org.zstack.header.message.APIEvent;

/**
 */
public class APIGenerateQueryableFieldsEvent extends APIEvent {
    private String outputFolder;

    public APIGenerateQueryableFieldsEvent(String apiId) {
        super(apiId);
    }

    public APIGenerateQueryableFieldsEvent() {
    }

    public String getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(String outputFolder) {
        this.outputFolder = outputFolder;
    }
 
    public static APIGenerateQueryableFieldsEvent __example__() {
        APIGenerateQueryableFieldsEvent event = new APIGenerateQueryableFieldsEvent();


        return event;
    }

}
