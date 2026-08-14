package org.zstack.zwatch.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.datatype.EventData;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(fieldsTo = {"all"})
public class APIGetEventDataReply extends APIReply {
    private List<EventData> events;

    private Long total;

    public static APIGetEventDataReply __example__() {
        APIGetEventDataReply reply = new APIGetEventDataReply();
        reply.setEvents(asList(EventData.__example__()));
        return reply;
    }

    public List<EventData> getEvents() {
        return events;
    }

    public void setEvents(List<EventData> events) {
        this.events = events;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
