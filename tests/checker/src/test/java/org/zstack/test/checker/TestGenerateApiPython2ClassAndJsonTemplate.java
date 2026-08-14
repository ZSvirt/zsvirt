package org.zstack.test.checker;

import org.junit.Test;
import org.zstack.configuration.ConfigurationManagerImpl;
import org.zstack.core.Platform;

/**
 * Temporarily hosted in checker (full Platform / Spring + DB).
 * Move back to tests/sdk-tools when offline generation is ready.
 * <p>
 * Called by "build_zstack_py_apis.sh" / {@code ./runMavenProfile py}.
 */
public class TestGenerateApiPython2ClassAndJsonTemplate {
    @Test
    public void test() throws Exception {
        System.setProperty("exitJVMOnBootFailure", "false");
        ConfigurationManagerImpl mgr = Platform.getComponentLoader()
                .getComponent(ConfigurationManagerImpl.class);
        mgr.start();
        mgr.generateApiJsonTemplate(null, null);
    }
}
