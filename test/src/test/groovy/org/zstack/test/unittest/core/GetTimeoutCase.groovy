package org.zstack.test.unittest.core

import org.junit.Test
import org.zstack.core.timeout.ApiTimeoutManagerImpl

/**
 * Create by weiwang at 2018/11/26 
 */
class GetTimeoutCase {
    @Test
    public void testParseObjectToLong() {
        Double a = 100.00
        double b = 100.00
        Long c = 100
        long d = 100
        assert ApiTimeoutManagerImpl.parseObjectToLong(a).equals(c)
        assert ApiTimeoutManagerImpl.parseObjectToLong(b).equals(c)
        assert ApiTimeoutManagerImpl.parseObjectToLong(c).equals(c)
        assert ApiTimeoutManagerImpl.parseObjectToLong(d).equals(c)
    }
}
