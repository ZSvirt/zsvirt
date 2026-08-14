package org.zstack.storage.zbs;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.header.Component;

/**
 * @author Xingwei Yu
 * @date 2024/4/4 16:25
 */
public class ZbsStorageAgentDeployer implements Component {
    @Autowired
    private AnsibleFacade asf;

    private void deployAnsible() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        asf.deployModule(ZbsGlobalProperty.PRIMARY_STORAGE_MODULE_PATH, ZbsGlobalProperty.PRIMARY_STORAGE_PLAYBOOK_NAME);
    }

    @Override
    public boolean start() {
        deployAnsible();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
