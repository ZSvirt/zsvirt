package org.zstack.premium.externalservice.prometheus.nodeexporter;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.externalservice.AbstractLocalExternalService;
import org.zstack.header.core.external.service.ExternalServiceCapabilities;
import org.zstack.core.externalservice.ExternalServiceCapabilitiesBuilder;
import org.zstack.utils.Bash;
import org.zstack.utils.path.PathUtil;

import java.util.ArrayList;
import java.util.List;

public class LocalNodeExporterImpl extends AbstractLocalExternalService implements LocalNodeExporter {
    private LocalNodeExporterParam param;

    static final String PARAM_LISTEN_ADDRESS = "-web.listen-address";
    static final String LOG_FILE = PathUtil.join(CoreGlobalProperty.LOG_DIR, "prometheus-node-exporter.log");

    ExternalServiceCapabilities capabilities = ExternalServiceCapabilitiesBuilder
            .build()
            .reloadConfig(false);

    public LocalNodeExporterImpl(LocalNodeExporterParam param) {
        param.checkParams();
        this.param = param;
    }

    @Override
    protected String[] getCommandLineKeywords() {
        return new String[] {"node_exporter", String.format("%s :%s", PARAM_LISTEN_ADDRESS, param.getPort())};
    }

    @Override
    public String getName() {
        return String.format("prometheus-node-exporter-on-%s:%s", Platform.getManagementServerIp(), param.getPort());
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

                sudoRun("chmod +x %s", param.getBinaryPath());

                List<String> params = new ArrayList<>(param.getParameters());
                params.add(String.format("%s :%s", PARAM_LISTEN_ADDRESS, param.getPort()));
                String pstr = StringUtils.join(params, " ");
                sudoRun("nohup sudo -b %s %s >%s 2>&1 < /dev/null", param.getBinaryPath(), pstr, LOG_FILE);
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
}
