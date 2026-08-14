package org.zstack.header.apimediator;

import java.util.List;

public interface GlobalApiMessageInterceptor extends ApiMessageInterceptor {
    enum InterceptorPosition {
        // In execution order
        SYSTEM, FRONT, DEFAULT, END
    }

    List<Class> getMessageClassToIntercept();

    /**
     * Sort by position, then {@link #getPriority()}
     */
    default InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }
}
