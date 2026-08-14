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
import java.util.LinkedHashMap;

/**
 * @author shenjin
 * @date 2024/12/2
 */
public class ForwardProxyFactory implements FluentBitOutputFactory {

    @Override
    public LogServerVO createFluentBitOutput(FluentBitConfigurationStruct struct, String accountUuid, String uuid, EncryptFacade encryptFacade) {
        ForwardConfigurationStruct forward = JSONObjectUtil.toObject(struct.getConfiguration(), ForwardConfigurationStruct.class);
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("name", "forward");
        if (AccountConstant.INITIAL_SYSTEM_ADMIN_UUID.equals(accountUuid)) {
            map.put("match", "account_*");
        } else {
            if (StringUtils.isBlank(accountUuid)) {
                throw new IllegalArgumentException("accountUuid can not be null or empty");
            }
            map.put("match", "account_" + accountUuid);
        }
        map.put("host", forward.host);
        map.put("port", forward.port);

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
