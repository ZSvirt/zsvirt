package org.zstack.utils.test;

import org.junit.Test;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;


public class TestCLogger {
    private static final CLogger _logger = CLoggerImpl.getLogger(TestCLogger.class);
    
    @Test
    public void test() {
        _logger.trace("This is a trace");
        _logger.debug("This is a debug");
        _logger.info("This is a info");
        _logger.warn("This is a warn");
    }

}
