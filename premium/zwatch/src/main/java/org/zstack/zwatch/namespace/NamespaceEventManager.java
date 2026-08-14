package org.zstack.zwatch.namespace;

import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.zwatch.driver.EventDatabaseDriver;

public interface NamespaceEventManager {
    EventDatabaseDriver getEventDatabaseDriver();
    void longJobAudit(LongJob job, LongJobVO vo, APIEvent evt);
}
