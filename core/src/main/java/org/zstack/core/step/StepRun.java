package org.zstack.core.step;

import org.zstack.core.asyncbatch.While;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mingjian.deng on 2019/1/30.
 */
public abstract class StepRun<T> {
    protected int stepLimit = 100;
    protected List<T> totalElements;

    private static final CLogger logger = Utils.getLogger(StepRun.class);

    protected abstract void call(List<T> stepElements, Completion completion);
    protected String __name__;

    public StepRun(List<T> totalElements) {
        this.totalElements = totalElements;
    }

    public void run(final Completion completion) {
        if (totalElements.isEmpty()) {
            completion.success();
            return;
        }

        Method m;
        try {
            m = getClass().getDeclaredMethod("call", List.class, Completion.class);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }

        StepRunCondition cond = m.getAnnotation(StepRunCondition.class);
        if (cond != null) {
            stepLimit = cond.stepLimit();
        }

        if (__name__ == null) {
            __name__ = getClass().getName();
        }

        List<List<T>> stepElements = new ArrayList<>();
        int index = 0;
        do {
            int end = index + stepLimit > totalElements.size() ? totalElements.size() : index + stepLimit;
            stepElements.add(totalElements.subList(index, end));
            index = end;
        } while (index < totalElements.size());

        new While<>(stepElements).each((stepElement, compl) -> {
            if (stepElement.isEmpty()) {
                compl.done();
                return;
            }
            call(stepElement, new Completion(compl) {
                @Override
                public void success() {
                    compl.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    compl.addError(errorCode);
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if(errorCodeList.getCauses().size() == stepElements.size()){
                    completion.fail(errorCodeList.getCauses().get(0));
                }else {
                    completion.success();
                }
            }
        });
    }

}