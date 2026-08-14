package org.zstack.core.cloudbus;

import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;

import java.util.ArrayList;
import java.util.List;

public class MessageIntegrityChecker implements Component {
    private void check() {
        List<String> badMessages = new ArrayList<String>();

        APIMessage.apiMessageClasses.forEach(clz -> {
            try {
                clz.getConstructor();
            } catch (NoSuchMethodException e) {
                badMessages.add(clz.getName());
            } catch (SecurityException e) {
                throw new CloudRuntimeException(e);
            }
        });
        
        if (!badMessages.isEmpty()) {
            throw new CloudRuntimeException(String.format("Message %s must have a public zero-parameter constructor", badMessages));
        }
    }
    
    @Override
    public boolean start() {
        check();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
