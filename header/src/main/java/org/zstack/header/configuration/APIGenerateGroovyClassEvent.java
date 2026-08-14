package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 10:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class APIGenerateGroovyClassEvent extends APIEvent {
    public APIGenerateGroovyClassEvent(String apiId) {
        super(apiId);
    }

    public APIGenerateGroovyClassEvent() {
        super(null);
    }
 
    public static APIGenerateGroovyClassEvent __example__() {
        APIGenerateGroovyClassEvent event = new APIGenerateGroovyClassEvent();


        return event;
    }

}
