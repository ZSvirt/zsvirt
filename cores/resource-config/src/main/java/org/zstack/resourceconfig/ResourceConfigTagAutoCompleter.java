package org.zstack.resourceconfig;

import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.configuration.ConfigAO;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIMessage;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.isEmpty;

/**
 * FRONT completer: peel {@code resourceConfig::…} from {@code systemTags} into {@code configs}
 * before validators and END interceptors. Does not validate values.
 */
public class ResourceConfigTagAutoCompleter implements GlobalApiMessageInterceptor {
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        List<String> systemTags = msg.getSystemTags();
        if (isEmpty(systemTags)) {
            return msg;
        }

        List<String> remainingTags = new ArrayList<>();
        List<ConfigAO> peeled = new ArrayList<>();
        for (String tag : systemTags) {
            if (!ConfigAO.match(tag)) {
                remainingTags.add(tag);
                continue;
            }
            peeled.add(ConfigAO.of(tag));
        }

        if (peeled.isEmpty()) {
            return msg;
        }

        List<ConfigAO> configs = msg.getConfigs();
        if (configs == null) {
            configs = new ArrayList<>();
            msg.setConfigs(configs);
        }
        configs.addAll(peeled);
        msg.setSystemTags(remainingTags);
        return msg;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return list(APICreateMessage.class);
    }

    @Override
    public int getPriority() {
        return -1;
    }
}
