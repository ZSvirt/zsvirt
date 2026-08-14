package org.zstack.header.cluster;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 5:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class ClusterException extends Exception {
    public ClusterException(String message) {
        super(message);
    }

    public ClusterException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClusterException(Throwable cause) {
        super(cause);
    }
}
