package org.zstack.test.integration.core.taskqueue;

import org.zstack.core.thread.SyncTask;

import java.util.Map;

public class TestSyncTask implements SyncTask<Void> {
    String syncSignature;
    String name;
    int level;
    Runnable runLogic;

    @Override
    public Map<String, String> getThreadContext() {
        return null;
    }

    /**
     * work around for aspectj wrong invoke in groovy
     *
     * see: HasThreadContextAspect.aj contains code
     *
     *     Map<String, String> around(HasThreadContext context) : target(context) && execution(Map<String, String> HasThreadContext+.getThreadContext()) {
     *         return context.threadContext;
     *     }
     *
     * when call getThreadContext() normally threadContext (variable will be returned) but
     * groovy failed to identify that and try to invoke threadContext() and throw
     * AbstractMethodError.
     *
     * note: change to gradle's aspectj could solve the issue, but we use maven so keep this
     * work around before any other solution is found
     *
     * @return null
     */
    public Map<String, String> threadContext() {
        return null;
    }

    TestSyncTask(String syncSignature, String name, int level, Runnable runLogic) {
        this.syncSignature = syncSignature;
        this.name = name;
        this.level = level;
        this.runLogic = runLogic;
    }

    @Override
    public String getSyncSignature() {
        return syncSignature;
    }

    @Override
    public int getSyncLevel() {
        return level;
    }

    @Override
    public String getName() {
        return syncSignature;
    }

    @Override
    public Void call() throws Exception {
        runLogic.run();
        return null;
    }
}
