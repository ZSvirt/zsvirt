package org.zstack.testlib.util

import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger
import java.util.concurrent.TimeUnit

/**
 * Created by lining on 2017/8/27.
 */
class TimeUnitUtil {
    static CLogger logger = Utils.getLogger(TimeUnitUtil.class)

    static int sleepSeconds(int sec){
        int sleepTime = sleepMilliseconds(sec * 1000) / 1000

        logger.info(String.format("Expect %s seconds of sleep, and the actual sleep time is %s seconds", sec, sleepTime))
        return sleepTime
    }

    static long sleepMilliseconds(long milliSec){
        long startTime = new Date().getTime()
        TimeUnit.MILLISECONDS.sleep(milliSec)
        long endTime = new  Date().getTime()

        return endTime - startTime
    }
}
