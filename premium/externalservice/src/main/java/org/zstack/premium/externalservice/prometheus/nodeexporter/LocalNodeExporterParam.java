package org.zstack.premium.externalservice.prometheus.nodeexporter;

import org.zstack.header.exception.CloudRuntimeException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LocalNodeExporterParam {
    private int port = 9101;
    private List<String> parameters = new ArrayList<>();
    private String binaryPath;

    void checkParams() {
        getParameters().forEach(p -> {
            if (p.contains(LocalNodeExporterImpl.PARAM_LISTEN_ADDRESS)) {
                throw new CloudRuntimeException(String.format("use LocalNodeExporterParam.port instead of %s", LocalNodeExporterImpl.PARAM_LISTEN_ADDRESS));
            }
        });

        if (binaryPath == null) {
            throw new CloudRuntimeException("binaryPath cannot be null");
        }

        if (!new File(binaryPath).exists()) {
            throw new CloudRuntimeException(String.format("cannot find node_exporter at %s", binaryPath));
        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    public String getBinaryPath() {
        return binaryPath;
    }

    public void setBinaryPath(String binaryPath) {
        this.binaryPath = binaryPath;
    }
}
