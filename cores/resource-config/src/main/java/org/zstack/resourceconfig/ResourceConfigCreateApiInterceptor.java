package org.zstack.resourceconfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.configuration.ConfigAO;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIMessage;

import java.util.List;

import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.isEmpty;

/**
 * Validates {@link APICreateMessage#getConfigs()} after
 * {@link ResourceConfigTagAutoCompleter} peels resourceConfig tags into configs (FRONT).
 */
public class ResourceConfigCreateApiInterceptor implements GlobalApiMessageInterceptor {
    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        List<ConfigAO> configs = msg.getConfigs();
        if (isEmpty(configs)) {
            return msg;
        }
        for (ConfigAO config : configs) {
            if (config == null) {
                continue;
            }
            validateResourceConfig(config);
        }
        return msg;
    }

    private void validateResourceConfig(ConfigAO config) {
        String identity = String.format("%s.%s", config.getCategory(), config.getName());
        ResourceConfig resourceConfig = rcf.getResourceConfig(identity);
        if (resourceConfig == null) {
            throw new ApiMessageInterceptionException(argerr(
                    "no resource config[category:%s, name:%s] found",
                    config.getCategory(), config.getName()));
        }
        try {
            resourceConfig.validateOnly(config.getValue());
        } catch (GlobalConfigException e) {
            throw new ApiMessageInterceptionException(argerr("failed to validate resource config")
                    .withOpaque("exception", e.getMessage()));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return list(APICreateMessage.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        // After FRONT ResourceConfigTagAutoCompleter peel
        return InterceptorPosition.END;
    }
}
