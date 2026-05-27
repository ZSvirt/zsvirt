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
public class LokiProxyFactory implements FluentBitOutputFactory {

    @Override
    public LogServerVO createFluentBitOutput(FluentBitConfigurationStruct struct, String accountUuid, String uuid, EncryptFacade encryptFacade) {
        LokiConfigurationStruct loki = JSONObjectUtil.toObject(struct.getConfiguration(), LokiConfigurationStruct.class);
        if (StringUtils.isBlank(accountUuid)) {
            throw new IllegalArgumentException("accountUuid can not be null or empty");
        }

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("name", "loki");
        if (AccountConstant.INITIAL_SYSTEM_ADMIN_UUID.equals(accountUuid)) {
            map.put("match", "account_*");
        } else {
            map.put("match", "account_" + accountUuid);
        }
        map.put("host", loki.host);
        map.put("port", loki.port);
        map.put("labels", loki.labels);
        map.put("line_format", "json");
        if (StringUtils.isNotEmpty(loki.httpUser)) {
            map.put("http_user", loki.httpUser);
            map.put("http_passwd", loki.httpPassword);
        }

        if (StringUtils.isNotEmpty(loki.tls)) {
            map.put("tls", loki.tls);
            map.put("tls.verify", "off");
        }

        LogServerVO vo = Q.New(LogServerVO.class).eq(LogServerVO_.uuid, uuid).find();
        if (vo == null) {
            throw new RuntimeException(String.format("LogServerVO[uuid:%s] not found", uuid));
        }
        // encrypt httpPassword before persisting to DB
        if (encryptFacade != null && StringUtils.isNotEmpty(loki.httpPassword)) {
            loki.setHttpPassword(encryptFacade.encrypt(loki.httpPassword));
        }
        FluentBitConfigurationStruct fluentStruct = FluentBitConfigurationStruct.fromJson(vo.getConfiguration());
        fluentStruct.setConfiguration(JSONObjectUtil.toJsonString(loki));
        vo.setConfiguration(JSONObjectUtil.toJsonString(fluentStruct));

        FluentBitServiceUnitConfig.addSingleFluentBitConfig(map);
        return vo;
    }

    @Override
    public OutPutType getFluentBitOutputType() {
        return OutPutType.Loki;
    }

    @Override
    public void validate(FluentBitConfigurationStruct struct) {
        LokiConfigurationStruct loki = JSONObjectUtil.toObject(struct.getConfiguration(), LokiConfigurationStruct.class);
        boolean isUsernameEmpty = StringUtils.isEmpty(loki.getHttpUser());
        boolean isPasswordEmpty = StringUtils.isEmpty(loki.getHttpPassword());
        if (loki.getLabels() == null) {
            throw new ApiMessageInterceptionException(argerr("loki labels can not be null"));
        }

        String regex = "^[a-zA-Z0-9_-]+\\s*=\\s*[a-zA-Z0-9_-]+$";
        if (!loki.getLabels().matches(regex)) {
            throw new ApiMessageInterceptionException(argerr("The format of Loki labels needs to be key=value"));
        }

        if (isUsernameEmpty != isPasswordEmpty) {
            throw new ApiMessageInterceptionException(argerr("username and password must be both empty or both non-empty"));
        }
    }
}
