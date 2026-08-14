package org.zstack.core.thread;

import org.zstack.header.core.AsyncBackup;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface SingleFlightTaskChain<T> extends AsyncBackup {
    void execute();
}
