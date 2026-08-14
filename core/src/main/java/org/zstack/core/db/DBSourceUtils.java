package org.zstack.core.db;

import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

public class DBSourceUtils {
    private static final CLogger logger = Utils.getLogger(DBSourceUtils.class);

    public static boolean isDBConnected() {
        try {
            new SQLBatch() {
                @Override
                protected void scripts() {
                    databaseFacade.getEntityManager().createNativeQuery("select 1").getSingleResult();
                }
            }.execute();

            logger.debug("the database is Connected");
            return true;
        } catch (Throwable t1) {
            logger.warn(String.format("unable to create a database connection, %s", t1.getMessage()), t1);
            return false;
        }
    }

    public static boolean waitDBConnected(int retryTimes, int interval) {
        for (int i = 0; i < retryTimes; i++) {
            if (isDBConnected()) {
                return true;
            }

            logger.warn("DB is still disconnected, retry it later");
            sleep(interval);
        }

        logger.warn("unable to create a database connection");
        return false;
    }


    private static void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }
    }
}
