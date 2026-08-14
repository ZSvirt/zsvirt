package org.zstack.testlib

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 * Created by xing5 on 2017/3/23.
 */
class FuncTrigger {
    private BlockingQueue queue = new LinkedBlockingQueue()
    private String quitObject = "quit"

    Closure func

    void trigger(o = "") {
        queue.add(o)

        synchronized (o) {
            o.wait()
        }
    }

    void quit() {
        queue.add(quitObject)
    }

    void run() {
        assert func != null: "func cannot be null"

        while (true) {
            def token = queue.take()
            if (token == quitObject) {
                break
            }

            func(token)

            synchronized (token) {
                token.notifyAll()
            }
        }
    }
}
