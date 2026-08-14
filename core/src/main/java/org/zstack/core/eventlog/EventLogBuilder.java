package org.zstack.core.eventlog;

import java.util.ArrayList;
import java.util.Collections;

public class EventLogBuilder {
    String content;
    String resourceUuid;
    String resourceType;
    String category;
    String trackingId;
    ArrayList<Object> arguments = new ArrayList<Object>();
    EventLogType type = EventLogType.Info;

    public EventLogBuilder resource(String uuid, String type) {
        this.resourceUuid = uuid;
        this.resourceType = type;
        return this;
    }

    public EventLogBuilder content(String content) {
        this.content = content;
        return this;
    }

    public EventLogBuilder type(EventLogType type) {
        this.type = type;
        return this;
    }

    public EventLogBuilder trackingId(String trackingId) {
        this.trackingId = trackingId;
        return this;
    }

    public EventLogBuilder category(String category) {
        this.category = category;
        return this;
    }

    public EventLogBuilder arguments(Object...args) {
        if (args != null) {
            Collections.addAll(this.arguments, args);
        }

        return this;
    }

}
