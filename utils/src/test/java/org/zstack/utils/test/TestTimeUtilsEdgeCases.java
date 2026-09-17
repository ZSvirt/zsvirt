package org.zstack.utils.test;

import org.junit.Test;
import org.zstack.utils.TimeUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestTimeUtilsEdgeCases {
    @Test
    public void testDefaultTimestampValidation() {
        assertTrue(TimeUtils.isValidTimestampFormat("2024-02-29 10:00:00"));
        assertFalse(TimeUtils.isValidTimestampFormat("2024-02-30 10:00:00"));
        assertFalse(TimeUtils.isValidTimestampFormat("2024-01-01 10:00:00xyz"));
    }

    @Test
    public void testCustomTimestampValidation() {
        assertTrue(TimeUtils.isValidTimestampFormat("2024/02/29", "yyyy/MM/dd"));
        assertFalse(TimeUtils.isValidTimestampFormat("2024/02/30", "yyyy/MM/dd"));
        assertFalse(TimeUtils.isValidTimestampFormat("2024/02/29 trailing", "yyyy/MM/dd"));
    }
}
