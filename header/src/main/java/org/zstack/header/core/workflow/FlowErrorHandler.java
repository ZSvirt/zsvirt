package org.zstack.header.core.workflow;

import org.zstack.header.core.AbstractCompletion;
import org.zstack.header.core.AsyncBackup;
import org.zstack.header.errorcode.ErrorCode;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 5:35 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class FlowErrorHandler extends AbstractCompletion {
    public FlowErrorHandler(AsyncBackup one, AsyncBackup... others) {
        super(one, others);
    }

    public abstract void handle(ErrorCode errCode, Map data);
}
