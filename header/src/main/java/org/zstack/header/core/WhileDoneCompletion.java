package org.zstack.header.core;

import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCodeList;

/**
 * Created by MaJin on 2021/1/26.
 */
public abstract class WhileDoneCompletion extends AbstractCompletion {
    public WhileDoneCompletion(AsyncBackup one, AsyncBackup... others) {
        super(one, others);
    }

    public abstract void done(ErrorCodeList errorCodeList);

    public static WhileDoneCompletion of(Completion completion) {
        return new WhileDoneCompletion(completion){
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.next(errorCodeList);
            }
        };
    }

    public static WhileDoneCompletion of(FlowTrigger trigger) {
        return new WhileDoneCompletion(trigger){
            @Override
            public void done(ErrorCodeList errorCodeList) {
                trigger.next(errorCodeList.wrapOrNull());
            }
        };
    }
}
