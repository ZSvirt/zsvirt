package org.zstack.core.timeout;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;

import javax.annotation.PostConstruct;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

public class TimeHelper implements Timer {
    @Autowired
    private ThreadFacade thdf;

    private volatile long currentTimeMillis = 0;

    @PostConstruct
    public void init() {
        thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return 1;
            }

            @Override
            public String getName() {
                return "update-current-time-millis-task";
            }

            @Override
            public void run() {
                currentTimeMillis = System.currentTimeMillis();
            }
        });
    }

    public long getCurrentTimeMillis() {
        return currentTimeMillis;
    }

    @Override
    public Timestamp getCurrentTimestamp() {
        return new Timestamp(currentTimeMillis);
    }
}
