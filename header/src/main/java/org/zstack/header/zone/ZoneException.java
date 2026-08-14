package org.zstack.header.zone;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 5:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class ZoneException extends Exception {
    public ZoneException(String message) {
        super(message);
    }

    public ZoneException(String message, Throwable cause) {
        super(message, cause);
    }

    public ZoneException(Throwable cause) {
        super(cause);
    }
}
