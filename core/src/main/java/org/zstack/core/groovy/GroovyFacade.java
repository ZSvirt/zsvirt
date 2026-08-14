package org.zstack.core.groovy;

import java.util.Map;

/**
 */
public interface GroovyFacade {
    void executeScriptByName(String scriptName, Map<Object, Object> context);

    void executeScriptByPath(String scriptName, Map<Object, Object> context);
}
