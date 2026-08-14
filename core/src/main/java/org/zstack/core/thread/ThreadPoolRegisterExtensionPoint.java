package org.zstack.core.thread;

import java.util.List;

public interface ThreadPoolRegisterExtensionPoint {
    List<ThreadPool> registerThreadPool();
}
