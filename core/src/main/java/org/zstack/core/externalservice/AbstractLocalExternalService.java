package org.zstack.core.externalservice;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.ProcessFinder;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class AbstractLocalExternalService implements LocalExternalService {
    protected static final CLogger logger = Utils.getLogger(AbstractLocalExternalService.class);

    protected abstract String[] getCommandLineKeywords();

    protected ProcessFinder processFinder = new ProcessFinder();

    public Integer getPID() {
        return processFinder.findByCommandLineKeywords(getCommandLineKeywords());
    }

    public void abortManagementNode(String error) {
        Platform.exit(error);
    }

    @Override
    public void stop() {
        Integer pid = getPID();
        if (pid != null) {
            Platform.killProcess(pid, 30);
            logger.debug(String.format("[External Service] stopped %s", getName()));
        } else {
            logger.debug(String.format("[External Service] not found %s, skip stop it", getName()));
        }
    }

    protected void doActionIfServicePIDNotShowUpInTwoMinutes(Runnable runnable) {
        ActionIfServiceNotUp a = new ActionIfServiceNotUp();
        a.action = runnable;
        a.upChecker = () -> getPID() != null;
        a.run();
    }

    @Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
    public class ActionIfServiceNotUp implements Runnable {
        public Integer timeout = 120; // in secs
        public int interval = 2; // in secs
        public Runnable action;
        public Callable<Boolean> upChecker;
        public int successTimes = 3;
        public Runnable actionAfterUp;

        private long endTime = System.currentTimeMillis();

        @Autowired
        protected ThreadFacade thdf;

        private void doAction() {
            action.run();
        }

        @AsyncThread
        private void doActionAfterUp() {
            if (actionAfterUp != null) {
                actionAfterUp.run();
            }
        }

        @Override
        public void run() {
            DebugUtils.Assert(action != null, "action cannot be null");
            DebugUtils.Assert(upChecker != null, "upChecker cannot be null");
            DebugUtils.Assert(timeout != null, "timeout cannot be null");

            endTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeout);

            thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
                private boolean checkServiceUp() throws Exception {
                    for (int i=0; i<successTimes; i++) {
                        if (!upChecker.call()) {
                            return false;
                        }

                        TimeUnit.SECONDS.sleep(interval);
                    }

                    return true;
                }

                @Override
                public boolean run() {
                    if (System.currentTimeMillis() >= endTime) {
                        // timeout
                        doAction();
                        return true;
                    }

                    try {
                        if (checkServiceUp()) {
                            // service up
                            doActionAfterUp();
                            return true;
                        }
                    } catch (Throwable t) {
                        logger.warn("unhandled exception", t);
                        return false;
                    }

                    // service not up, continue
                    return false;
                }

                @Override
                public TimeUnit getTimeUnit() {
                    return TimeUnit.SECONDS;
                }

                @Override
                public long getInterval() {
                    return interval;
                }

                @Override
                public String getName() {
                    return "wait-for-external-service-up";
                }
            });
        }
    }


    public void restart() {
        stop();
        start();
    }
}
