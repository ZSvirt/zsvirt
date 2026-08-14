package org.zstack.configuration;

import java.util.List;

/**
 */
public interface TypeScriptApiWriter {
    void write(String outPath, List<Class> apiClass, List<Class> apiResultClass, List<Class> inventoryClass);
}
