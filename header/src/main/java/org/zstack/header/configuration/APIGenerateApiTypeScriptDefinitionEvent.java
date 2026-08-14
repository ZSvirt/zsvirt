package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;

/**
 */
public class APIGenerateApiTypeScriptDefinitionEvent extends APIEvent {
    public APIGenerateApiTypeScriptDefinitionEvent(String apiId) {
        super(apiId);
    }

    public APIGenerateApiTypeScriptDefinitionEvent() {
        super(null);
    }
 
    public static APIGenerateApiTypeScriptDefinitionEvent __example__() {
        APIGenerateApiTypeScriptDefinitionEvent event = new APIGenerateApiTypeScriptDefinitionEvent();


        return event;
    }

}
