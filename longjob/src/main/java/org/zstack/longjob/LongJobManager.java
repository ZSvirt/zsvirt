package org.zstack.longjob;

import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.SafeConsumer;
import org.zstack.header.longjob.SubmitLongJobMsg;
import org.zstack.header.message.APIEvent;

/**
 * Created by GuoYi on 11/14/17.
 */
public interface LongJobManager {
    void loadLongJob();
    void submitLongJob(SubmitLongJobMsg msg, CloudBusCallBack submitCallBack, SafeConsumer<APIEvent> jobCallBack);
}
