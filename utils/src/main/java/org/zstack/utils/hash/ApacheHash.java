package org.zstack.utils.hash;

import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 10:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApacheHash implements HashFunction {
    @Override
    public int hash(Object obj) {
        return new HashCodeBuilder().append(obj).hashCode();
    }
}
