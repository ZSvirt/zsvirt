package org.zstack.premium.externalservice.fluentbit.output;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.Q;
import org.zstack.core.encrypt.EncryptFacade;
import org.zstack.header.identity.AccountConstant;
import org.zstack.log.server.LogServerVO;
import org.zstack.log.server.LogServerVO_;
import org.zstack.premium.externalservice.fluentbit.FluentBitConfigurationStruct;
import org.zstack.premium.externalservice.fluentbit.FluentBitServiceUnitConfig;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.Map;

/**
 * @author shenjin
 * @date 2024/12/2
 */
public class ForwardProxyFactory implements FluentBitOutputFactory {

    @Override
    public LogServerVO createFluentBitOutput(FluentBitConfigurationStruct struct, String accountUuid, String uuid, EncryptFacade encryptFacade) {
        if (!AccountConstant.INITIAL_SYSTEM_ADMIN_UUID.equals(accountUuid)) {
            if (StringUtils.isBlank(accountUuid)) {
                throw new IllegalArgumentException("accountUuid can not be null or empty");
            }
        }
        Map<String, Object> map = FluentBitOutputConfigHelper.makeOutputConfig(OutPutType.Forward,
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
        return OutPutType.Forward;
    }

    @Override
    public void validate(FluentBitConfigurationStruct struct) {
    }
}
