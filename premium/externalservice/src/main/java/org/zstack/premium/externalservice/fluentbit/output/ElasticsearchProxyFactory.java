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

public class ElasticsearchProxyFactory implements FluentBitOutputFactory {

    @Override
    public LogServerVO createFluentBitOutput(FluentBitConfigurationStruct struct, String accountUuid, String uuid, EncryptFacade encryptFacade) {
        Map<String, Object> configuration = FluentBitOutputConfigHelper.normalizeConfiguration(
                JSONObjectUtil.toObject(struct.getConfiguration(), Map.class));
        ElasticsearchConfigurationStruct es = JSONObjectUtil.toObject(JSONObjectUtil.toJsonString(configuration), ElasticsearchConfigurationStruct.class);
        if (StringUtils.isBlank(accountUuid)) {
            throw new IllegalArgumentException("accountUuid can not be null or empty");
        }

        Map<String, Object> map = FluentBitOutputConfigHelper.makeOutputConfig(OutPutType.Elasticsearch, configuration, accountUuid);

        LogServerVO vo = Q.New(LogServerVO.class).eq(LogServerVO_.uuid, uuid).find();
        if (vo == null) {
            throw new RuntimeException(String.format("LogServerVO[uuid:%s] not found", uuid));
        }
        // encrypt httpPassword before persisting to DB
        if (encryptFacade != null && StringUtils.isNotEmpty(es.httpPassword)) {
            es.setHttpPassword(encryptFacade.encrypt(es.httpPassword));
        }
        FluentBitConfigurationStruct fluentStruct = FluentBitConfigurationStruct.fromJson(vo.getConfiguration());
        fluentStruct.setConfiguration(JSONObjectUtil.toJsonString(es));
        vo.setConfiguration(JSONObjectUtil.toJsonString(fluentStruct));

        FluentBitServiceUnitConfig.addSingleFluentBitConfig(map);
        return vo;
    }

    @Override
    public OutPutType getFluentBitOutputType() {
        return OutPutType.Elasticsearch;
    }

    @Override
    public void validate(FluentBitConfigurationStruct struct) {
        Map<String, Object> configuration = FluentBitOutputConfigHelper.normalizeConfiguration(
                JSONObjectUtil.toObject(struct.getConfiguration(), Map.class));
        ElasticsearchConfigurationStruct es = JSONObjectUtil.toObject(JSONObjectUtil.toJsonString(configuration), ElasticsearchConfigurationStruct.class);
        if (StringUtils.isEmpty(es.index)) {
            throw new ApiMessageInterceptionException(argerr("index can not be null"));
        }
        boolean isUsernameEmpty = StringUtils.isEmpty(es.getHttpUser());
        boolean isPasswordEmpty = StringUtils.isEmpty(es.getHttpPassword());

        if (isUsernameEmpty != isPasswordEmpty) {
            throw new ApiMessageInterceptionException(argerr("username and password must be both empty or both non-empty"));
        }
    }
}
