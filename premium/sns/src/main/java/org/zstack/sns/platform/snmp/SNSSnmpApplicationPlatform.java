package org.zstack.sns.platform.snmp;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vo.ResourceVO;
import org.zstack.sns.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 *
 * @Author : jingwang
 * @create 2023/8/24 14:20
 */
public class SNSSnmpApplicationPlatform extends SNSApplicationPlatformBase {
    @Autowired
    private CloudBus bus;

    public SNSSnmpApplicationPlatform(SNSApplicationPlatformVO self) {
        super(self);
    }

    @Override
    protected SNSSnmpPlatformVO getSelf() {
        return (SNSSnmpPlatformVO) self;
    }

    @Override
    protected SNSSnmpPlatformInventory getInventory() {
        return SNSSnmpPlatformInventory.valueOf(getSelf());
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdateSNSSnmpPlatformMsg) {
            handle((APIUpdateSNSSnmpPlatformMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    private void handle(APIUpdateSNSSnmpPlatformMsg msg) {
        APIUpdateSNSApplicationPlatformEvent event = new APIUpdateSNSApplicationPlatformEvent(msg.getId());
        boolean update = false;
        if (msg.getName() != null) {
            getSelf().setName(msg.getName());
        }
        if (msg.getDescription() != null) {
            getSelf().setDescription(msg.getDescription());
        }
        if (msg.getSnmpAddress() != null) {
            getSelf().setSnmpAddress(msg.getSnmpAddress());
            update = true;
        }
        if (msg.getSnmpPort() != null) {
            getSelf().setSnmpPort(msg.getSnmpPort());
            update = true;
        }
        if (update) {
            self = dbf.updateAndRefresh(getSelf());
        }
        event.setInventory(SNSSnmpPlatformInventory.valueOf(getSelf()));
        bus.publish(event);
    }

    @Override
    public void publish(SNSTopicInventory topic, MessageStruct message, ReturnValueCompletion<List<SNSPublishError>> completion) {
        List<SNSApplicationEndpointVO> endpointVOS = SQL.New("select e from SNSApplicationEndpointVO e, SNSSubscriberVO s" +
                " where e.uuid = s.endpointUuid and s.topicUuid = :topicUuid and e.type = :endpointType")
                .param("topicUuid", topic.getUuid())
                .param("endpointType", getSelf().getType())
                .list();

        if (endpointVOS.isEmpty()) {
            completion.success(null);
            return;
        }

        List<SNSPublishError> errors = new ArrayList<>();

        List<SNSApplicationEndpointVO> enabled = new ArrayList<>();
        List<SNSApplicationEndpointVO> disabled = new ArrayList<>();
        endpointVOS.forEach(vo -> {
            if (vo.getState() == SNSApplicationEndpointState.Enabled) {
                enabled.add(vo);
            } else {
                disabled.add(vo);
            }
        });

        disabled.forEach(vo -> {
            SNSPublishError error = new SNSPublishError();
            error.setEndpointUuid(vo.getUuid());
            error.setPlatformType(self.getType());
            error.setPlatformUuid(self.getUuid());
            error.setError(operr("the endpoint is disabled"));
            errors.add(error);
        });

        List<String> endpointUuids = enabled.stream().map(ResourceVO::getUuid).collect(Collectors.toList());

        SNSApplicationEndpointFactory f = snsMgr.getSNSApplicationEndpointFactory(self.getType());
        List<SNSApplicationEndpoint> endpoints = f.getSNSApplicationEndpoints(endpointUuids);
        new While<>(endpoints).each((item, c) -> item.publish(message, new Completion(c) {
            @Override
            public void success() {
                c.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                SNSPublishError error = new SNSPublishError();
                error.setEndpointUuid(item.getInventory().getPlatformUuid());
                error.setPlatformType(self.getType());
                error.setPlatformUuid(self.getUuid());
                error.setError(errorCode);
                errors.add(error);
                c.done();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success(errors);
            }
        });
    }
}
