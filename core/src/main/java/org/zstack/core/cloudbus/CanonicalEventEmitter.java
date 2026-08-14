package org.zstack.core.cloudbus;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CanonicalEventEmitter {
    @Autowired
    private EventFacade eventf;

    public void fire(String path, Object data) {
        eventf.fire(path, data);
    }
}
