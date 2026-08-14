package org.zstack.cloudformation.template.decoder;

import org.zstack.cloudformation.template.struct.CloudFormationConstants;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.APIMessage;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2018/7/11.
 */
public class DecoderUtils {
    private static final CLogger logger = Utils.getLogger(DecoderUtils.class);

    public static String getCreateAction(String resource) {
        String action = resource;
        String prefix = CloudFormationConstants.resourceCreatePrefix.get(resource);
        if (prefix != null) {
            action = prefix + action;
        } else {
            action = CloudFormationConstants.createPrefix + action;
        }

        String suffix = CloudFormationConstants.resourceCreateSuffix.get(resource);
        if (suffix != null) {
            action = action + suffix + CloudFormationConstants.actionSuffix;
        } else {
            action = action + CloudFormationConstants.actionSuffix;
        }
        return action;
    }

    public static String getMsgFromResourceType(String resource, String actionType) {
        String msg = resource;
        if (actionType.equals("Resource")) {
            String prefix = CloudFormationConstants.resourceCreatePrefix.get(resource);
            if (prefix != null) {
                msg = CloudFormationConstants.apiPrefix + prefix + msg;
            } else {
                msg = CloudFormationConstants.apiPrefix + CloudFormationConstants.createPrefix + msg;
            }
            String suffix = CloudFormationConstants.resourceCreateSuffix.get(resource);
            if (suffix != null) {
                msg = msg + suffix + CloudFormationConstants.msgSuffix;
            } else {
                msg = msg + CloudFormationConstants.msgSuffix;
            }
            return msg;
        } else {
            return CloudFormationConstants.apiPrefix + msg + CloudFormationConstants.msgSuffix;
        }

    }

    public static String getResourceTypeInMsg(String key, String msg) {
        Map<Class, Collection<APIMessage.FieldParam>> tmp = APIMessage.getApiParams();
        Map<String, Collection<APIMessage.FieldParam>> tmp1 = new HashMap<>();
        tmp.forEach((key1, value) -> {
            tmp1.put(key1.getSimpleName(), value);
        });
        if (tmp1.get(msg) == null) {
            throw new OperationFailureException(operr("cannot find such msg: %s for create", msg));
        }
        for (APIMessage.FieldParam param: tmp1.get(msg)) {
            if (key.equals(param.field.getName())) {
                final Class<?>[] classes = param.param.resourceType();
                if (classes.length == 0) {
                    continue;
                }

                String clz = classes[0].getSimpleName(); // TODO (ZSphere may not support cloudformation soon)
                if (clz.lastIndexOf("VO") == -1) {
                    continue;
                }
                return clz.substring(0, clz.lastIndexOf("VO"));
            }
        }
        return null;
    }

    public static List<String> getResourcesTypeInMsg(String msg) {
        List<String> resources = new ArrayList<>();
        Map<Class, Collection<APIMessage.FieldParam>> tmp = APIMessage.getApiParams();
        Map<String, Collection<APIMessage.FieldParam>> tmp1 = new HashMap<>();
        tmp.forEach((key1, value) -> {
            tmp1.put(key1.getSimpleName(), value);
        });
        if (tmp1.get(msg) == null) {
            throw new OperationFailureException(operr("cannot find such msg: %s for create", msg));
        }
        for (APIMessage.FieldParam param: tmp1.get(msg)) {
            final Class<?>[] classes = param.param.resourceType();
            if (classes.length == 0) {
                continue;
            }

            String clz = classes[0].getSimpleName();
            if (clz.endsWith("VO")) {
                resources.add(clz.substring(0, clz.lastIndexOf("VO")));
            } else if (clz.endsWith("EO")) {
                resources.add(clz.substring(0, clz.lastIndexOf("EO")));
            }
        }
        return resources;
    }
}
