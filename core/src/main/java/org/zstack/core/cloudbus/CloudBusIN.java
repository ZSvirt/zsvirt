package org.zstack.core.cloudbus;

import org.zstack.header.Service;

public interface CloudBusIN extends CloudBus {
    void activeService(Service serv);

    void activeService(String id);
    
    void deActiveService(Service serv);
    
    void deActiveService(String id);
}
