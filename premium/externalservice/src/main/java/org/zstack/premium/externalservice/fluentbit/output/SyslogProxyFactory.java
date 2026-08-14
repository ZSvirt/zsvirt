package org.zstack.premium.externalservice.fluentbit.output;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.Q;
import org.zstack.core.encrypt.EncryptFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.log.server.LogServerVO;
import org.zstack.log.server.LogServerVO_;
import org.zstack.premium.externalservice.fluentbit.FluentBitConfigurationStruct;
import org.zstack.premium.externalservice.fluentbit.FluentBitServiceUnitConfig;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.Map;

import static org.zstack.core.Platform.argerr;

/**
 * @author shenjin
 * @date 2024/12/2
 */
public class SyslogProxyFactory implements FluentBitOutputFactory {

    @Override
    public LogServerVO createFluentBitOutput(FluentBitConfigurationStruct struct, String accountUuid, String uuid, EncryptFacade encryptFacade) {
        if (StringUtils.isBlank(accountUuid)) {
            throw new IllegalArgumentException("accountUuid can not be null or empty");
        }

        Map<String, Object> map = FluentBitOutputConfigHelper.makeOutputConfig(OutPutType.Syslog,
                JSONObjectUtil.toObject(struct.getConfiguration(), Map.class), accountUuid);

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
