package org.zstack.core.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xing5 on 2016/7/25.
 */
public interface DebugManager {
    Map<String, List<DebugSignalHandler>> sigHandlers = new HashMap<>();

    static void registerDebugSignalHandler(String sig, DebugSignalHandler handler) {
        List<DebugSignalHandler> hs = sigHandlers.computeIfAbsent(sig, k -> new ArrayList<>());
        hs.add(handler);
    }

    default List<String> getDebugSignals() {
        return new ArrayList<>(sigHandlers.keySet());
    }

    void handleSig(String sig);
}
