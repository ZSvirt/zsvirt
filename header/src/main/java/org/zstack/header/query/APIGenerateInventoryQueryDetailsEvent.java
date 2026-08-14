package org.zstack.header.query;

import org.zstack.header.message.APIEvent;

public class APIGenerateInventoryQueryDetailsEvent extends APIEvent {
    private String outputDir;

    public APIGenerateInventoryQueryDetailsEvent(String apiId) {
        super(apiId);
    }

    public APIGenerateInventoryQueryDetailsEvent() {
        super(null);
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }
 
    public static APIGenerateInventoryQueryDetailsEvent __example__() {
        APIGenerateInventoryQueryDetailsEvent event = new APIGenerateInventoryQueryDetailsEvent();


        return event;
    }

}
