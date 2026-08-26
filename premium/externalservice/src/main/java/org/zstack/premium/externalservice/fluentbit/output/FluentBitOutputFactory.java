package org.zstack.premium.externalservice.fluentbit.output;

import org.zstack.core.encrypt.EncryptFacade;
import org.zstack.log.server.LogServerVO;
import org.zstack.premium.externalservice.fluentbit.FluentBitConfigurationStruct;

/**
 * @author shenjin
 * @date 2024/12/2
 */
public interface FluentBitOutputFactory {
    LogServerVO createFluentBitOutput(FluentBitConfigurationStruct struct, String accountUuid, String uuid, EncryptFacade encryptFacade);

    OutPutType getFluentBitOutputType();

    void validate(FluentBitConfigurationStruct struct);
}
