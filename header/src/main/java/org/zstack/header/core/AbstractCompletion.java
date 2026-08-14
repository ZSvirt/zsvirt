package org.zstack.header.core;

import org.zstack.header.HasThreadContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Arrays.asList;

/**
 */
public abstract class AbstractCompletion implements AsyncBackup, HasThreadContext {
    protected List<AsyncBackup> backups;
    private final AtomicBoolean successCalled = new AtomicBoolean(false);
    private final AtomicBoolean failCalled = new AtomicBoolean(false);

    public final List<AsyncBackup> getBackups() {
        return backups;
    }

    public final void setBackups(List<AsyncBackup> backups) {
        this.backups = backups;
    }

    protected AbstractCompletion(AsyncBackup one, AsyncBackup... others) {
        if (one != null) {
            backups = new ArrayList<>();
            backups.add(one);
            Collections.addAll(backups, others);
        } else {
            backups = asList(others);
        }
    }

    public final AtomicBoolean getSuccessCalled() {
        return successCalled;
    }

    public final AtomicBoolean getFailCalled() {
        return failCalled;
    }
}
