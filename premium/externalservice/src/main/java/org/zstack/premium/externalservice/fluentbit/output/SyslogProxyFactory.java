package org.zstack.premium.externalservice.fluentbit.output;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.Q;
import org.zstack.core.encrypt.EncryptFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.identity.AccountConstant;
import org.zstack.log.server.LogServerVO;
import org.zstack.log.server.LogServerVO_;
import org.zstack.premium.externalservice.fluentbit.FluentBitConfigurationStruct;
import org.zstack.premium.externalservice.fluentbit.FluentBitServiceUnitConfig;
import org.zstack.utils.gson.JSONObjectUtil;
import java.util.LinkedHashMap;
import static org.zstack.core.Platform.argerr;

/**
 * @author shenjin
 * @date 2024/12/2
 */
public class SyslogProxyFactory implements FluentBitOutputFactory {

    @Override
    public LogServerVO createFluentBitOutput(FluentBitConfigurationStruct struct, String accountUuid, String uuid, EncryptFacade encryptFacade) {
        SyslogConfigurationStruct syslog = JSONObjectUtil.toObject(struct.getConfiguration(), SyslogConfigurationStruct.class);
        if (StringUtils.isBlank(accountUuid)) {
            throw new IllegalArgumentException("accountUuid can not be null or empty");
        }

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("name", "syslog");
        if (AccountConstant.INITIAL_SYSTEM_ADMIN_UUID.equals(accountUuid)) {
            map.put("match", "account_*");
        } else {
            map.put("match", "account_" + accountUuid);
        }
        map.put("host", syslog.host);
        map.put("port", syslog.port);
        map.put("mode", syslog.mode);
        map.put("syslog_format", "rfc5424");
        map.put("syslog_maxsize", "2048");
        map.put("syslog_message_key", "platformOperationLog");

        LogServerVO vo = Q.New(LogServerVO.class).eq(LogServerVO_.uuid, uuid).find();
        if (vo == null) {
            throw new RuntimeException(String.format("LogServerVO[uuid:%s] not found", uuid));
        }
        FluentBitConfigurationStruct fluentStruct = FluentBitConfigurationStruct.fromJson(vo.getConfiguration());
        fluentStruct.setConfiguration(JSONObjectUtil.toJsonString(map));
        vo.setConfiguration(JSONObjectUtil.toJsonString(fluentStruct));

        FluentBitServiceUnitConfig.addSingleFluentBitConfig(map);
        return vo;
    }

    @Override
    public OutPutType getFluentBitOutputType() {
        return OutPutType.Syslog;
    }

    @Override
    public void validate(FluentBitConfigurationStruct struct) {
        SyslogConfigurationStruct syslog = JSONObjectUtil.toObject(struct.getConfiguration(), SyslogConfigurationStruct.class);
        if (syslog.getMode() == null) {
            throw new ApiMessageInterceptionException(argerr("syslog mode can not be null"));
        }
    }
}