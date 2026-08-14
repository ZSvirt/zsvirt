package org.zstack.premium.externalservice.loki;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.externalservice.ExternalServiceFactory;
import org.zstack.core.externalservice.ExternalServiceManager;
import org.zstack.core.externalservice.ExternalServiceType;
import org.zstack.header.Component;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;

/**
 * Created by mingjian.deng on 2019/9/6.
 */
public class LokiFactory implements ExternalServiceFactory, Component {
    protected static final CLogger logger = Utils.getLogger(LokiFactory.class);
    public static final ExternalServiceType type = new ExternalServiceType("Loki");
    @Autowired
    private ExternalServiceManager manager;
    @Autowired
    private AnsibleFacade asf;

    private Loki instance;
    private Loki needCloseInstance;

    @Override
    public String getExternalServiceType() {
        return type.toString();
    }

    private void initialLoki() {
        instance = (Loki)manager.getService(new LokiImpl03().getName(), LokiImpl03::new);
    }

    private void closeLoki() {
        needCloseInstance = (Loki)manager.getService(new LokiImpl03().getName(), LokiImpl03::new);
    }

    @Override
    public boolean start() {
        if ( ! System.getProperty("os.arch").equals("amd64")) {
            return true;
        }
        String mode = LokiGlobalProperty.LOKI_VERSION_MODE;
        switch (mode) {
            case "0.3.0": {
                initialLoki();
                break;
            }
            case "none": {
                closeLoki();
                break;
            }
            default: {
                throw new CloudRuntimeException(String.format("invalid value [%s] of GlobalProperty Loki.versionMode has been found!", mode));
            }
        }
        if (needCloseInstance != null) {
            needCloseInstance.stop();
        }

        if (instance != null) {
            instance.start();
        }

        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            String dir = "ansible/promtail";
            File src = PathUtil.findFileOnClassPath("tools/promtail", true);
            File dst = PathUtil.findFolderOnClassPath(dir, true);
            // withSudo must be false
            ShellResult rst = ShellUtils.runAndReturn(String.format(
                    "/usr/bin/cp -f %s %s", src.getAbsolutePath(), dst.getAbsolutePath()
            ), false);
            if (rst.getRetCode() != 0) {
                throw new OperationFailureException(Platform.operr(
                        "cannot copy %s to %s, caused: %s", src.getAbsolutePath(), dst.getAbsolutePath(), rst.getStderr()
                ));
            }
            asf.deployModule("ansible/promtail", LokiGlobalProperty.PROMTAIL_PLAYBOOK_NAME);
        }

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
