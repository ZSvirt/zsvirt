package org.zstack.testlib

/**
 * Created by xing5 on 2017/2/21.
 */
class HttpError extends Exception {
    int status
    String message

    HttpError(int status, String message) {
        this.status = status
        this.message = message
    }
}
