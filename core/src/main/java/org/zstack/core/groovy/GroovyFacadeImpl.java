package org.zstack.core.groovy;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.util.Map;

/**
 */
public class GroovyFacadeImpl implements GroovyFacade {
    @Override
    public void executeScriptByName(String scriptName, Map<Object, Object> context) {
        File script = PathUtil.findFileOnClassPath(scriptName, true);
        executeScriptByPath(script.getAbsolutePath(), context);
    }

    @Override
    public void executeScriptByPath(String scriptPath, Map<Object, Object> context) {
        try {
            String scriptName = PathUtil.fileName(scriptPath);
            String scriptDir = PathUtil.parentFolder(scriptPath);

            GroovyScriptEngine gse = new GroovyScriptEngine(scriptDir);
            Binding binding = new Binding(context);
            gse.run(scriptName, binding);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
}
