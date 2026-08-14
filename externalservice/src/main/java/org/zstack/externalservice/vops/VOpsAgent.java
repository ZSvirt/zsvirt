package org.zstack.externalservice.vops;

import org.zstack.core.Platform;
import org.zstack.core.externalservice.AbstractLocalExternalService;
import org.zstack.core.externalservice.ExternalServiceCapabilitiesBuilder;
import org.zstack.header.core.external.service.ExternalServiceCapabilities;
import org.zstack.utils.Bash;

/**
 * Note: VOps is a handler by systemctl.
 * It is always running after MN installed.
 *
 * So it is why we don't need to write "VOpsFactory" class
 */
public class VOpsAgent extends AbstractLocalExternalService {
    @Override
    protected String[] getCommandLineKeywords() {
        return new String[]{"/usr/bin/python3", "/usr/local/vops/vops-agent/setup.py"};
    }

    ExternalServiceCapabilities capabilities = ExternalServiceCapabilitiesBuilder
            .build()
            .reloadConfig(false);

    @Override
    public String getName() {
        return "vops-agent";
    }

    @Override
    public void start() {
        if (isAlive()) {
            return;
        }

        new Bash() {
            @Override
            protected void scripts() {
                setE();
                sudoRun("systemctl start vops");
            }
        }.execute();
    }

    @Override
    public boolean isAlive() {
        return getPID() != null;
    }

    @Override
    public ExternalServiceCapabilities getExternalServiceCapabilities() {
        return capabilities;
    }

    @Override
    public void reload() {
        // do nothing
    }

    public VOpsClient createClient() {
        return Platform.New(VOpsClient::new);
    }
}
