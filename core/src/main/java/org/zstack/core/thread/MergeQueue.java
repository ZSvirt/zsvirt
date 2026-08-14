package org.zstack.core.thread;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Created by mingjian.deng on 2020/4/29.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class MergeQueue {
    private static final CLogger logger = Utils.getLogger(MergeQueue.class);

    @Autowired
    protected ThreadFacade thdf;

    private String name;
    private Supplier supplier;
    private static Map<String, AtomicInteger> counter = new ConcurrentHashMap<>();
    private int syncLevel = 1;


    public MergeQueue addTask(String n, Supplier s) {
        name = n;
        supplier = s;
        return this;
    }

    public MergeQueue setSyncLevel(int level) {
        syncLevel = level;
        return this;
    }

    public void run() {
        DebugUtils.Assert(name != null, "addTask() must be called");

        if (counter.get(name) != null && counter.get(name).intValue() > syncLevel) {
            return;
        }
        counter.compute(name, (k, v) -> {
            if (v == null) {
                return new AtomicInteger(1);
            }
            v.incrementAndGet();
            return v;
        });
        logger.debug(String.format("%s counter: %s, sync level: %s", name, counter.toString(), syncLevel));


        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return name;
            }

            @Override
            public void run(SyncTaskChain chain) {
                supplier.get();
                counter.computeIfPresent(name, (k, v) -> {
                    int d = v.decrementAndGet();
                    if (d == 0) {
                        return null;
                    }
                    return v;
                });
                chain.next();
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            protected int getSyncLevel() {
                return syncLevel;
            }
        });
    }
}
