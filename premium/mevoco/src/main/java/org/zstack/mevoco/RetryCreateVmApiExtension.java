package org.zstack.mevoco;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.header.Component;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.*;
import org.zstack.header.vm.APICreateVmInstanceEvent;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.APIStartVmInstanceEvent;
import org.zstack.header.vm.APIStartVmInstanceMsg;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by frank on 10/22/2015.
 */
public class RetryCreateVmApiExtension implements Component {
    private static final CLogger logger = Utils.getLogger(RetryCreateVmApiExtension.class);

    @Autowired
    private CloudBus bus;

    private ConcurrentHashMap<String, ApiStruct> apis = new ConcurrentHashMap<String, ApiStruct>();

    private class ApiStruct {
        Message msg;
        int retryTimes;
        int interval;
    }

    @Override
    public boolean start() {
        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            public void beforeDeliveryMessage(Message msg) {
                if (msg instanceof APICreateVmInstanceMsg) {
                    record(msg);
                } else if (msg instanceof APIStartVmInstanceMsg) {
                    record(msg);
                }
            }
        }, APICreateVmInstanceMsg.class, APIStartVmInstanceMsg.class);

        bus.installBeforePublishEventInterceptor(new AbstractBeforePublishEventInterceptor() {
            @Override
            public void beforePublishEvent(Event evt) {
                if (evt instanceof APIEvent) {
                    retry((APIEvent) evt);
                }
            }
        }, APICreateVmInstanceEvent.class, APIStartVmInstanceEvent.class);

        return true;
    }

    private void record(Message msg) {
        if (MevocoGlobalConfig.VM_API_RETRY.value(Integer.class) <= 0)  {
            return;
        }

        if (apis.containsKey(msg.getId())) {
            // still retrying
            return;
        }

        ApiStruct s = new ApiStruct();
        s.msg = msg;
        s.retryTimes = MevocoGlobalConfig.VM_API_RETRY.value(Integer.class);
        s.interval = MevocoGlobalConfig.VM_API_RETRY_INTERVAL.value(Integer.class);
        apis.put(msg.getId(), s);
    }

    private void retry(APIEvent evt) {
        if (!apis.containsKey(evt.getApiId())) {
            return;
        }

        if (evt.isSuccess()) {
            apis.remove(evt.getApiId());
            return;
        }

        ApiStruct s = apis.get(evt.getApiId());
        if (s.retryTimes-- < 0) {
            // let the event go
            apis.remove(evt.getApiId());
            return;
        }

        try {
            TimeUnit.SECONDS.sleep(s.interval);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CloudRuntimeException(e);
        }

        logger.warn(String.format("API call failed, now is going to retry:\n" +
                "left retry times: %s\n" +
                "msg: %s\n" +
                "event: %s\n", s.retryTimes, JSONObjectUtil.toJsonString(s.msg), JSONObjectUtil.toJsonString(evt)));
        s.msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        bus.send(s.msg);
        throw new StopRoutingException();
    }

    @Override
    public boolean stop() {
        return true;
    }
}
