package org.zstack.core.externalservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.header.AbstractService;
import org.zstack.header.core.external.service.APIGetExternalServicesMsg;
import org.zstack.header.core.external.service.APIGetExternalServicesReply;
import org.zstack.header.core.external.service.APIReloadExternalServiceEvent;
import org.zstack.header.core.external.service.APIReloadExternalServiceMsg;
import org.zstack.header.core.external.service.ExternalServiceInventory;
import org.zstack.header.core.external.service.ExternalServiceStatus;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static org.zstack.core.Platform.operr;

public class ExternalServiceManagerImpl extends AbstractService implements ExternalServiceManager {
    @Autowired
    public CloudBus bus;

    private final Map<String, ExternalService> services = new ConcurrentHashMap<>();

    @Override
    public ExternalService registerService(ExternalService service) {
        if (services.containsKey(service.getName())) {
            throw new OperationFailureException(operr("service[%s] has been registered", service.getName()));
        }

        services.put(service.getName(), service);
        return service;
    }

    @Override
    public void deregisterService(String name) {
        services.remove(name);
    }

    @Override
    public ExternalService getService(String name) {
        return services.get(name);
    }

    @Override
    public ExternalService getService(String name, Supplier<ExternalService> supplier) {
        ExternalService service = services.get(name);
        if (service != null) {
            return service;
        }

        service = supplier.get();
        return registerService(service);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    public void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetExternalServicesMsg) {
            handle((APIGetExternalServicesMsg) msg);
        } else if (msg instanceof APIReloadExternalServiceMsg) {
            handle((APIReloadExternalServiceMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIReloadExternalServiceMsg msg) {
        APIReloadExternalServiceEvent event = new APIReloadExternalServiceEvent(msg.getId());
        ExternalService service = services.get(msg.getName());
        if (service == null) {
            event.setError(operr("service[%s] is not registered", msg.getName()));
            bus.publish(event);
            return;
        }

        if (!service.getExternalServiceCapabilities().isReloadConfig()) {
            event.setError(operr("service[%s] does not support reload config", msg.getName()));
        }

        if (service.isAlive()) {
            service.reload();
        } else {
            event.setError(operr("service[%s] is not running", msg.getName()));
        }

        bus.publish(event);
    }

    private void handle(APIGetExternalServicesMsg msg) {
        APIGetExternalServicesReply reply = new APIGetExternalServicesReply();
        reply.setInventories(new ArrayList<>());

        services.forEach((name, service) -> {
            ExternalServiceInventory inv = new ExternalServiceInventory();
            inv.setName(name);
            inv.setStatus(service.isAlive() ? ExternalServiceStatus.RUNNING.toString() : ExternalServiceStatus.STOPPED.toString());
            inv.setCapabilities(service.getExternalServiceCapabilities());
            reply.getInventories().add(inv);
        });

        bus.reply(msg, reply);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }
}
