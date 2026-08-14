package org.zstack.header.message;

/**
 * Created by frank on 10/15/2015.
 */
public abstract class AbstractBeforeDeliveryMessageInterceptor implements BeforeDeliveryMessageInterceptor {
    public int orderOfBeforeDeliveryMessageInterceptor() {
        return 0;
    }
}
