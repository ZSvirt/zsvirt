package org.zstack.premium.externalservice.loki;

import org.zstack.core.externalservice.AbstractLocalExternalService;
import org.zstack.core.externalservice.LocalServiceUnitConfig;
import org.zstack.utils.Bash;

/**
 * Created by mingjian.deng on 2019/9/9.
 */
public abstract class AbstractLokiImpl extends AbstractLocalExternalService implements Loki {
    abstract protected String getProcessName();
    abstract public LocalServiceUnitConfig getConfigurations();

    private void sysctl(String ctl) {
        new Bash() {
            @Override
            protected void scripts() {
                setE();
                sudoRun("systemctl %s %s", ctl, getProcessName());
            }
        }.execute();
    }

    @Override
    public boolean isAlive() {
        return getPID() != null;
    }

    @Override
    public void stop() {
        if (!isAlive()) {
            return;
        }
        sysctl("stop");
    }

    @Override
    public void restart() {
        sysctl("restart");
    }

    @Override
    public void start() {
        if (isAlive()) {
            return;
        }

        prepare();
        sysctl("restart");
    }

    protected void prepare() {
        LocalServiceUnitConfig config = getConfigurations();
        new Bash() {
            @Override
            protected void scripts() {
                unsetE();
                run("touch %s", config.getConfigFilePath());
                writeFile(config.getConfigFilePath(), config.getConfigFileContent());
                sudoRunScripts("sync");
            }
        }.execute();


        new Bash() {
            @Override
            protected void scripts() {
                unsetE();
                sudoRunScripts("touch", config.getServiceUnitPath());
                sudoRunScripts("chmod", "777", config.getServiceUnitPath());
                sudoRunFormat("echo '%s' > %s", config.getServiceUnitContent(), config.getServiceUnitPath());
                sudoRunScripts("chmod", "644", config.getServiceUnitPath());
                sudoRunScripts("sync");
                run("for i in 1 2 3; do sudo systemctl daemon-reload && break || sleep 5; done");
            }
        }.execute();
    }
}
