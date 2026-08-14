package org.zstack.header.core.workflow;

import java.util.Map;

/**
 * Created by xing5 on 2016/4/29.
 */
public class NopeFlow implements Flow {
    String __name__ = "nope";

    @Override
    public void run(FlowTrigger trigger, Map data) {
        trigger.next();
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        trigger.rollback();
    }
}
