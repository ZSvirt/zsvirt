package org.zstack.core.asyncbatch;

import org.zstack.core.Platform;
import org.zstack.core.progress.ProgressWhileProcessorFactory;
import org.zstack.core.thread.ThreadGlobalProperty;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.WhileCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2017/3/5.
 */

public class While<T> {
    private static final CLogger logger = Utils.getLogger(While.class);
    private Collection<T> items;
    private Do consumer;

    private int step;
    private WhileMode mode;

    private AtomicBoolean isOver = new AtomicBoolean(false);
    private AtomicInteger doneCount = new AtomicInteger(0);

    private ErrorCodeList errors = new ErrorCodeList();
    private List<WhileProcessor> processors;

    public interface Do<T> {
        void accept(T item, WhileCompletion completion);
    }

    public While(Collection<T> items) {
        this.items = items;
        doneCount.set(items.size());
    }

    public static While<Integer> makeRetryWhile(int retryCount) {
        int[] array = new int[retryCount];
        for(int i = 0; i < array.length; i++) {
            array[i] = i + 1;
        }
        return new While<>(Arrays.stream(array).mapToObj(Integer::valueOf).collect(Collectors.toList()));
    }

    public While<T> enableProgressReport(String content) {
        try {
            ProgressWhileProcessorFactory factory = Platform.getComponentLoader().getComponent(ProgressWhileProcessorFactory.class);
            if (processors == null) {
                processors = new ArrayList<>();
            }
            processors.add(factory.create(content));
        } catch (Exception e) {
            logger.warn(String.format("unable to create progress while processor factory: %s", e.getMessage()));
        }
        return this;
    }

    private void callProcessorStart() {
        if (processors != null) {
            CollectionUtils.safeForEach(processors, p -> p.beforeStart(items.size()));
        }
    }

    private void callProcessorDone() {
        if (processors != null) {
            CollectionUtils.safeForEach(processors, p -> p.afterDone(items.size() - doneCount.get()));
        }
    }

    private void callProcessorAllDone() {
        if (processors != null) {
            CollectionUtils.safeForEach(processors, WhileProcessor::afterAllDone);
        }
    }

    public While each(Do<T> consumer) {
        mode = WhileMode.EACH;
        this.consumer = consumer;
        return this;
    }

    @Deprecated
    public While all(Do<T> consumer) {
        this.mode = WhileMode.STEP;
        this.consumer = consumer;

        int concurrencyLevel = WhileGlobalProperty.CONCURRENCY_LEVEL_OF_ALL_MODE;
        if (concurrencyLevel <= 0) {
            this.step = items.size();
        } else {
            this.step = ThreadGlobalProperty.MAX_THREAD_NUM * concurrencyLevel / 100;
        }

        this.step = this.step == 0 ? 1 : this.step;
        return this;
    }

    private void run(Iterator<T> it, WhileDoneCompletion completion) {
        if (!it.hasNext()) {
            callProcessorAllDone();
            completion.done(errors);
            return;
        }

        T t = it.next();
        consumer.accept(t, new WhileCompletion(completion) {
            @Override
            public void allDone() {
                callProcessorAllDone();
                completion.done(errors);
            }

            @Override
            public void addError(ErrorCode error) {
                errors.add(error);
            }

            @Override
            public void done() {
                run(it, completion);
            }
        });
    }

    public While step(Do<T> consumer, int step) {
        if (step < 0) {
            throw new IllegalArgumentException(String.format("step must be greater than zero, got %s", step));
        }

        this.consumer = consumer;
        this.step = step;
        mode = WhileMode.STEP;
        return this;
    }

    public void run(WhileDoneCompletion completion) {
        DebugUtils.Assert(consumer != null, "each() or all() or step() must be called before run()");
        callProcessorStart();

        if (items.isEmpty()) {
            callProcessorAllDone();
            completion.done(errors);
            return;
        }

        switch (mode) {
            case EACH: {
                run(items.iterator(), completion);
            }
            break;
            case STEP: {
                runStep(completion);
            }
            break;
            default:
                DebugUtils.Assert(false, "should not be here");
                break;
        }
    }

    private void runStep(WhileDoneCompletion completion) {
        int s = Math.min(step, items.size());

        Iterator<T> it = items.iterator();
        for (int i = 0; i < s; i++) {
            runStep(it, completion);
        }
    }

    private void runStep(Iterator<T> it, WhileDoneCompletion completion) {
        T t;
        synchronized (it) {
            if (!it.hasNext() || isOver.get()) {
                return;
            }

            t = it.next();
        }

        consumer.accept(t, new WhileCompletion(completion) {
            @Override
            public void allDone() {
                doneCompletion(completion);
            }

            @Override
            public void addError(ErrorCode error) {
                errors.add(error);
            }

            @Override
            public void done() {
                callProcessorDone();
                if (doneCount.decrementAndGet() == 0) {
                    doneCompletion(completion);
                    return;
                }
                runStep(it, completion);
            }
        });
    }

    private void doneCompletion(WhileDoneCompletion completion) {
        if (isOver.compareAndSet(false, true)) {
            callProcessorAllDone();
            completion.done(errors);
        }
    }
}
