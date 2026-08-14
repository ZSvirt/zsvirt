package org.zstack.rest;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by xing5 on 2017/9/15.
 */
public interface RestServletRequestInterceptor {
    class RestServletRequestInterceptorException extends Exception {
        public int statusCode;
        public String error;

        public RestServletRequestInterceptorException(int statusCode, String error) {
            this.statusCode = statusCode;
            this.error = error;
        }
    }

    void intercept(HttpServletRequest req) throws RestServletRequestInterceptorException;
}
