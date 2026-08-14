package org.zstack.core.thread;

import org.zstack.header.core.AsyncBackup;
import org.zstack.utils.DebugUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RunInQueue {
    private String name;
    private List<AsyncBackup> asyncBackups = new ArrayList<>();
    private int syncLevel;

    private String id;
    protected ThreadFacade thdf;

    public RunInQueue name(String v) {
        name = v;
        return this;
    }

    public RunInQueue asyncBackup(AsyncBackup v) {
        asyncBackups.add(v);
        return this;
    }

    public RunInQueue(String id, ThreadFacade thdf, int syncLevel) {
        this.id = id;
        this.thdf = thdf;
        this.syncLevel = syncLevel;
    }

    public void run(Consumer<SyncTaskChain> consumer) {
        DebugUtils.Assert(name != null, "name() must be called");
        DebugUtils.Assert(!asyncBackups.isEmpty(), "asyncBackup must be called");

        AsyncBackup one = asyncBackups.get(0);
        AsyncBackup[] rest = asyncBackups.size() > 1 ?
                asyncBackups.subList(1, asyncBackups.size()).toArray(new AsyncBackup[asyncBackups.size() - 1]) :
                new AsyncBackup[0];

        thdf.chainSubmit(new ChainTask(one, rest) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(SyncTaskChain chain) {
                consumer.accept(chain);
            }

            @Override
            protected int getSyncLevel() {
                return syncLevel;
            }

            @Override
            public String getName() {
                return name;
            }
        });
    }
}
