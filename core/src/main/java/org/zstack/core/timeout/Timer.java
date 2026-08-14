package org.zstack.core.timeout;

import java.sql.Timestamp;

public interface Timer {
    long getCurrentTimeMillis();
    Timestamp getCurrentTimestamp();
}
