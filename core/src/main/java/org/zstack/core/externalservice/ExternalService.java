package org.zstack.core.externalservice;

import org.zstack.header.core.external.service.ExternalServiceCapabilities;

public interface ExternalService {
    String getName();

    void start();

    void stop();

    void restart();

    boolean isAlive();

    ExternalServiceCapabilities getExternalServiceCapabilities();

    void reload();
}
