package org.zstack.testlib.util

import org.zstack.testlib.StopTestSuiteException
import org.zstack.utils.Utils

import java.util.concurrent.TimeUnit

trait Retry {
    /**
     *
     * @param total
     * @param interval: NOTE, default is 1/10 second for fast executing
     * @param c
     * @return
     */
    boolean retryInSecs(int total = 8, int interval = 1, Closure c) {
        int count = 0
        def ret = false

        while ((count / 1000) < total) {
            try {
                def r = c()
                ret = r == null || (r != null && r instanceof Boolean && r)
            } catch (StopTestSuiteException e) {
                throw e
            } catch (Throwable t) {
                if (count % 1000 == 0) {
                    Utils.getLogger(Retry.class).debug("[retryInSecs:${(count / 1000) + 1}/${total}]", t)
                }
                if (total - (count / 1000) == 1) {
                    throw t
                }
            }

            if (ret) {
                return ret
            }
            TimeUnit.MILLISECONDS.sleep(interval * 100)
            count += 100
        }

        return false
    }
}