package org.zstack.ha.spi;

import org.zstack.header.core.HaCompletion;

import java.util.Collection;

public interface HaCompletionCacher {
    boolean enqueCompletion(final HaCompletion job, final String key);
    Collection<HaCompletion> dequeCompletions(String key);
}
