package org.zstack.utils.hash;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 10:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class JavaHash implements HashFunction {
    @Override
    public int hash(Object obj) {
        return obj.hashCode();
    }
}
