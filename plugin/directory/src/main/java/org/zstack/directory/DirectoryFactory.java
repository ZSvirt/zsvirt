package org.zstack.directory;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author shenjin
 * @date 2022/12/12 12:41
 */
public class DirectoryFactory implements Component {
    @Autowired
    private PluginRegistry pluginRgty;

    Map<String, DirectoryChecker> directoryCheckers = Collections.synchronizedMap(new HashMap<String, DirectoryChecker>());

    @Override
    public boolean start() {
        populateExtensions();
        return false;
    }

    @Override
    public boolean stop() {
        return false;
    }

    private void populateExtensions() {
        for (DirectoryChecker checker : pluginRgty.getExtensionList(DirectoryChecker.class)) {
            DirectoryChecker old = directoryCheckers.get(checker.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate DirectoryChecker[%s, %s] for type[%s]",
                        checker.getClass().getName(), old.getClass().getName(), checker.getType()));
            }
            directoryCheckers.put(checker.getType().toString(), checker);
        }
    }
}
