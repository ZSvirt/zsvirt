package org.zstack.test;

import org.zstack.core.asyncbatch.While;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.zstack.core.Platform.operr;

/**
 * Created by MaJin on 2021/1/27.
 */
public class TestSafeWhile {
    public void testSafeWhile() {
        // *(.., WhileCompletion, ..) will catch the exception, and call the addError.
        FutureCompletion fc = new FutureCompletion(null);
        new While<>(Arrays.asList(1, 2, 3)).each((item, completion) -> {
            throw new OperationFailureException(operr("on purpose %d", item));
        }).run(new WhileDoneCompletion(fc) {
            @Override
            public void done(ErrorCodeList errs) {
                assert errs.getCauses().size() == 3 : "errors:" + errs.getCauses().toString();
                fc.success();
            }
        });

        fc.await();
        assert fc.isSuccess();

        // *(.., WhileCompletion, ..) will catch the exception, but addError will only be called once.
        FutureCompletion fc2 = new FutureCompletion(null);
        new While<>(Arrays.asList(1, 2, 3)).each((item, completion) -> {
            completion.addError(operr("on purpose %d", item));
            completion.addError(operr("I should not be in error list %d", item));
            throw new OperationFailureException(operr("I should not be in error list either %d", item));
        }).run(new WhileDoneCompletion(fc2) {
            @Override
            public void done(ErrorCodeList errs) {
                assert errs.getCauses().size() == 3 : "errors:" + errs.getCauses().toString();
                assert errs.getCauses().stream().allMatch(it -> it.getDetails().startsWith("on purpose"));
                fc2.success();
            }
        });

        fc2.await();
        assert fc2.isSuccess();

        // WhileDoneCompletion(asyncBackup async).done() will handle the exception and asyncBackup will be called.
        FutureCompletion fc3 = new FutureCompletion(null);
        new While<>(Arrays.asList(1, 2, 3)).each((item, completion) -> {
            completion.addError(operr("on purpose %d", item));
            completion.done();
        }).run(new WhileDoneCompletion(fc3) {
            @Override
            public void done(ErrorCodeList errs) {
                assert errs.getCauses().size() == 3 : "errors:" + errs.getCauses().toString();
                assert errs.getCauses().stream().allMatch(it -> it.getDetails().startsWith("on purpose"));
                throw new OperationFailureException(operr("done, on purpose"));
            }
        });

        fc3.await();
        assert !fc3.isSuccess();
        assert fc3.getErrorCode().getDetails().equals("done, on purpose") : "details: " + fc3.getErrorCode().getDetails();

        // WhileDoneCompletion(WhileCompletion async).done() will handle the exception
        // WhileCompletion.addError() and done() will be called.
        FutureCompletion fc4 = new FutureCompletion(null);
        new While<>(Arrays.asList(1, 2, 3)).each((item, completion) -> {
            new While<>(Arrays.asList(1, 2, 3)).each((innerItem, innerCompl) -> {
                innerCompl.done();
            }).run(new WhileDoneCompletion(completion) {
                @Override
                public void done(ErrorCodeList errorCodeList) {
                    completion.addError(operr("on purpose"));
                    completion.addError(operr("I should not be errs list"));
                    throw new OperationFailureException(operr("I should not be errs list either."));
                }
            });
        }).run(new WhileDoneCompletion(fc4) {
            @Override
            public void done(ErrorCodeList errs) {
                assert errs.getCauses().size() == 3 : "errors:" + errs.getCauses().toString();
                assert errs.getCauses().stream().allMatch(it -> it.getDetails().startsWith("on purpose"));
                fc4.success();
            }
        });

        fc4.await();
        assert fc4.isSuccess();
    }
}
