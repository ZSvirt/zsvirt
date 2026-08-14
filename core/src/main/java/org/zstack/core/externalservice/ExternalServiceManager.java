package org.zstack.core.externalservice;

import java.util.function.Supplier;

public interface ExternalServiceManager {
    String SERVICE_ID = "externalService";

    ExternalService registerService(ExternalService service);

    void deregisterService(String name);

    ExternalService getService(String name);

    /**
     * return an existing external service, or register a new one then return
     *
     * if the service of the @name is registered, return the registered one
     * otherwise, use @supplier to get an service instance then register it and return
     *
     * @param name service name
     * @param supplier factory interface which provides an external service instance if the service of the @name
     *                 is not registered.
     * @return
     */
    ExternalService getService(String name, Supplier<ExternalService> supplier);
}
