package org.zstack.core.thread;

import org.zstack.header.PassMaskWords;
import org.zstack.header.HasThreadContext;

import java.util.concurrent.Callable;

public interface Task<T> extends Callable<T>, HasThreadContext, PassMaskWords {
    String getName();
}
