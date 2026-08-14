package org.zstack.zwatch.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.namespace.VmNamespace;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "events")
public class APIGetAllEventMetadataReply extends APIReply {
    public static class EventStruct {
        public String namespace;
        public String name;
        public String description;
        public List<String> labelNames;

        public static EventStruct __example__() {
            EventStruct ret = new EventStruct();
            ret.namespace = "ZStack/VM";
            ret.name = VmNamespace.VMHAProcess.getName();
            ret.description = "VMHAProcess";
            ret.labelNames = VmNamespace.VMHAProcess.getLabelNames();
            return ret;
        }
    }

    public static APIGetAllEventMetadataReply __example__() {
        APIGetAllEventMetadataReply ret = new APIGetAllEventMetadataReply();
        ret.setEvents(asList(EventStruct.__example__()));
        return ret;
    }

    private List<EventStruct> events;

    public List<EventStruct> getEvents() {
        return events;
    }

    public void setEvents(List<EventStruct> events) {
        this.events = events;
    }
}
