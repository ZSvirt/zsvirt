package org.zstack.snmp;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.message.APIMessage;
import org.zstack.snmp.agent.*;

import java.util.List;
import java.util.Objects;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

/**
 * @Author : jingwang
 * @create 2023/8/1 19:27
 */
@InterceptorForService("SNMP")
public class SnmpApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    DatabaseFacade dbf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateSnmpAgentMsg) {
            validate((APICreateSnmpAgentMsg) msg);
        } else if (msg instanceof APIStartSnmpAgentMsg) {
            validate((APIStartSnmpAgentMsg) msg);
        } else if (msg instanceof APIUpdateSnmpAgentMsg) {
            validate((APIUpdateSnmpAgentMsg) msg);
        } else if (msg instanceof APIStopSnmpAgentMsg) {
            validate((APIStopSnmpAgentMsg) msg);
        }
        return msg;
    }

    private void validate(APICreateSnmpAgentMsg msg) {
        final List<SnmpAgentVO> lst = dbf.listAll(SnmpAgentVO.class);
        if (lst.size() > 0) {
            throw new ApiMessageInterceptionException(operr("Failed to create SNMP agent, because snmp agent already created."));
        }

        checkSecurityInfo(msg, false);
    }

    private void validate(APIStopSnmpAgentMsg msg) {
        final List<SnmpAgentVO> lst = dbf.listAll(SnmpAgentVO.class);
        if (lst.size() < 1) {
            throw new ApiMessageInterceptionException(operr("Failed to stop SNMP agent, please create a snmp agent first."));
        }
    }

    private void validate(APIUpdateSnmpAgentMsg msg) {
        final List<SnmpAgentVO> lst = dbf.listAll(SnmpAgentVO.class);
        if (lst.size() < 1) {
            throw new ApiMessageInterceptionException(operr("Failed to update SNMP agent, please create a snmp agent first."));
        }
        checkSecurityInfo(msg, true);
    }

    private void validate(APIStartSnmpAgentMsg msg) {
        final List<SnmpAgentVO> lst = dbf.listAll(SnmpAgentVO.class);
        if (lst.size() < 1) {
            throw new ApiMessageInterceptionException(operr("Failed to start SNMP agent, please create a snmp agent first."));
        }
    }

    private void checkSecurityInfo(NewSnmpAgentMessage msg, boolean update) {
        String operation;
        if (update) {
            operation = "update";
        } else {
            operation = "create";
        }

        if (Objects.equals(msg.getVersion(), SnmpAgentVersion.v2c.toString())
            && StringUtils.isBlank(msg.getReadCommunity())) {
            throw new ApiMessageInterceptionException(argerr("Failed to %s SNMP agent, " +
                    "because readCommunity can not be empty when version is v2c", operation));
        }
        if (Objects.equals(msg.getVersion(), SnmpAgentVersion.v3.toString())) {
            if (StringUtils.isBlank(msg.getUserName())) {
                throw new ApiMessageInterceptionException(argerr("Failed to %s SNMP agent, " +
                        "because userName can not be empty when version is v3", operation));
            }
            if (StringUtils.isBlank(msg.getAuthAlgorithm()) && StringUtils.isNotBlank(msg.getAuthPassword())) {
                throw new ApiMessageInterceptionException(argerr("Failed to %s SNMP agent, " +
                        "auth algorithm can not be null when password is not null.", operation));
            }
            if (StringUtils.isNotBlank(msg.getAuthAlgorithm()) && StringUtils.isBlank(msg.getAuthPassword())) {
                throw new ApiMessageInterceptionException(argerr("Failed to %s SNMP agent, " +
                        "because auth password can not be empty.", operation));
            }
            if (StringUtils.isBlank(msg.getAuthAlgorithm()) && StringUtils.isNotBlank(msg.getPrivacyAlgorithm())) {
                throw new ApiMessageInterceptionException(argerr("Failed to %s SNMP agent, " +
                        "because setting data encryption requires setting user verification first.", operation));
            }
            if (StringUtils.isBlank(msg.getPrivacyAlgorithm()) && StringUtils.isNotBlank(msg.getPrivacyPassword())) {
                throw new ApiMessageInterceptionException(argerr("Failed to %s SNMP agent, " +
                        "because privacy password can not be empty.", operation));
            }
            if (StringUtils.isNotBlank(msg.getPrivacyAlgorithm()) && StringUtils.isBlank(msg.getPrivacyPassword())) {
                throw new ApiMessageInterceptionException(argerr("Failed to %s SNMP agent, " +
                        "because privacy password can not be empty.", operation));
            }
        }
    }
}
