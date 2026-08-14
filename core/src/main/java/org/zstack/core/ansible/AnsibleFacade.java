package org.zstack.core.ansible;

import java.util.Map;

/**
 */
public interface AnsibleFacade {
    void deployModule(String modulePath, String playBookName);

    boolean isModuleChanged(String playbookName);

    Map<String, String> getVariables();

    String getPublicKey();

    String getPrivateKey();
}
