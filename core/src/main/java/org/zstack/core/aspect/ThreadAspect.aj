package org.zstack.core.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.thread.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public aspect ThreadAspect {
    @Autowired
    ThreadFacade thread;

    private static final CLogger logger = Utils.getLogger(ThreadAspect.class);

    pointcut syncThread(Object entity) : target(entity) && execution(@org.zstack.core.thread.SyncThread void java.lang.Object+.*(..));
    
    pointcut syncThreadFuture(Object entity) : target(entity) && execution(@org.zstack.core.thread.SyncThread Future<java.lang.Void> java.lang.Object+.*(..));

    pointcut asyncThread() : execution(@org.zstack.core.thread.AsyncThread void java.lang.Object+.*(..));
    
    pointcut asyncThreadFuture() : execution(@org.zstack.core.thread.AsyncThread Future<java.lang.Void> java.lang.Object+.*(..));

    pointcut scheduledThread() : execution(@org.zstack.core.thread.ScheduledThread void java.lang.Object+.*(..));
    
    pointcut scheduledThreadFuture() : execution(@org.zstack.core.thread.ScheduledThread Future<java.lang.Void> java.lang.Object+.*(..));

    private String getSyncSignature(Object entity, JoinPoint point, SyncThread at) {
        String signature;
        String pointSignature = point.getSignature().toLongString();
        if (entity instanceof SyncThreadSignature) {
            signature = ((SyncThreadSignature) entity).getSyncSignature();
            assert signature != null : entity.getClass().getName() + ".getSyncSignature() must return none null string";
            if (at.compoundSignature()) {
                if (!"".equals(at.signature())) {
                    signature = signature + "." + at.signature();
                } else {
                    signature = signature + "." + pointSignature;
                }
            }
        } else {
            if (at.compoundSignature() && !"".equals(at.signature())) {
                signature = at.signature() + "." + pointSignature;
            } else if (!at.compoundSignature() && !"".equals(at.signature())) {
                signature = at.signature();
            } else {
                signature = pointSignature;
            }
        }

        return signature;
    }
    
    Future<Void> around(final Object entity) : syncThreadFuture(entity) {
    	 MethodSignature mtd = (MethodSignature) thisJoinPoint.getStaticPart().getSignature();
         final SyncThread at = mtd.getMethod().getAnnotation(SyncThread.class);
         final String signature = getSyncSignature(entity, thisJoinPoint, at);

         return thread.syncSubmit(new SyncTask<Void>() {
             @Override
             public Void call() throws Exception {
                 proceed(entity);
                 return null;
             }

             @Override
             public String getName() {
                 StringBuilder info = new StringBuilder();
                 info.append("SyncTask").append("[").append(signature).append("]").append(": ");
                 info.append(thisJoinPoint.getSignature());
                 return info.toString();
             }

             @Override
             public int getSyncLevel() {
                 return at.level();
             }

             @Override
             public String getSyncSignature() {
                 return signature;
             }
         });
    }
    
    void around(final Object entity) : syncThread(entity) {
        MethodSignature mtd = (MethodSignature) thisJoinPoint.getStaticPart().getSignature();
        final SyncThread at = mtd.getMethod().getAnnotation(SyncThread.class);
        final String signature = getSyncSignature(entity, thisJoinPoint, at);

        thread.syncSubmit(new SyncTask<Void>() {
            @Override
            public Void call() throws Exception {
                proceed(entity);
                return null;
            }

            @Override
            public String getName() {
                StringBuilder info = new StringBuilder();
                info.append("SyncTask").append("[").append(signature).append("]").append(": ");
                info.append(thisJoinPoint.getSignature());
                return info.toString();
            }

            @Override
            public int getSyncLevel() {
                return at.level();
            }

            @Override
            public String getSyncSignature() {
                return signature;
            }
        });
    }

    
    Future<Void> around() : asyncThreadFuture() {
        return thread.submit(new Task<Void>() {
            @Override
            public Void call() throws Exception {
                proceed();
                return null;
            }

            @Override
            public String getName() {
                StringBuilder info = new StringBuilder();
                info.append("ASyncTask").append(": ");
                info.append(thisJoinPoint.getSignature());
                return info.toString();
            }
        });
    }
    
    void around() : asyncThread() {
        thread.submit(new Task<Void>() {
            @Override
            public Void call() throws Exception {
                proceed();
                return null;
            }

            @Override
            public String getName() {
                StringBuilder info = new StringBuilder();
                info.append("ASyncTask").append(": ");
                info.append(thisJoinPoint.getSignature());
                return info.toString();
            }
        });
    }
    
    Future<Void> around() : scheduledThreadFuture() {
        MethodSignature mtd = (MethodSignature) thisJoinPoint.getStaticPart().getSignature();
        final ScheduledThread at = mtd.getMethod().getAnnotation(ScheduledThread.class);
        return thread.submitPeriodicTask(new PeriodicTask() {
            @Override
            public void run() {
                proceed();
            }

            @Override
            public TimeUnit getTimeUnit() {
                return at.timeUnit();
            }

            @Override
            public long getInterval() {
                return at.interval();
            }

            @Override
            public String getName() {
                StringBuilder info = new StringBuilder();
                info.append("ScheduledTask: ").append("[").append("Interval=").append(at.interval()).append(", TimeUnit=").append(at.timeUnit().toString())
                        .append(", delay=").append(at.delay()).append("] ");
                info.append(thisJoinPoint.getSignature());
                return info.toString();
            }

        }, at.delay());
    }

    void around() : scheduledThread() {
        MethodSignature mtd = (MethodSignature) thisJoinPoint.getStaticPart().getSignature();
        final ScheduledThread at = mtd.getMethod().getAnnotation(ScheduledThread.class);
        thread.submitPeriodicTask(new PeriodicTask() {
            @Override
            public void run() {
                proceed();
            }

            @Override
            public TimeUnit getTimeUnit() {
                return at.timeUnit();
            }

            @Override
            public long getInterval() {
                return at.interval();
            }

            @Override
            public String getName() {
                StringBuilder info = new StringBuilder();
                info.append("ScheduledTask: ").append("[").append("Interval=").append(at.interval()).append(", TimeUnit=").append(at.timeUnit().toString())
                        .append(", delay=").append(at.delay()).append("] ");
                info.append(thisJoinPoint.getSignature());
                return info.toString();
            }

        }, at.delay());
    }
}
