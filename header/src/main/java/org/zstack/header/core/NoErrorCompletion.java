package org.zstack.header.core;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 8:46 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class NoErrorCompletion extends AbstractCompletion {
    public NoErrorCompletion(AsyncBackup... completion) {
        super(null, completion);
    }

    public abstract void done();
}
