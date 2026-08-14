package org.zstack.cloudformation;

import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang.StringUtils;
import org.zstack.cloudformation.template.CloudFormationDecoder;
import org.zstack.cloudformation.template.struct.CfnResults;
import org.zstack.cloudformation.template.struct.ResultStruct;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.sdk.CreateSystemTagAction;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Field;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2018/6/28.
 */
public class CloudFormationUtils {
    private static final CLogger logger = Utils.getLogger(CloudFormationUtils.class);

    public static void validateTemplate(String content, boolean decode) {
        validateTemplate(content, null, null, decode);
    }

    private static void validateResults(final CfnResults result) {
        if (result == null) {
            throw new OperationFailureException(operr("get null element in template content"));
        }
        try {
            if (result.getTemplateVersion() == null || result.getTemplateVersion().isEmpty()) {
                throw new OperationFailureException(operr("template must contain [ZStackTemplateFormatVersion]"));
            }
            if (!result.getTemplateVersion().equals(CloudFormationConstant.version)) {
                throw new OperationFailureException(operr("invalid ZStackTemplateFormatVersion: [%s, expected: %s]",
                        result.getTemplateVersion(), CloudFormationConstant.version));
            }
        } catch (JsonSyntaxException e) {
            throw new OperationFailureException(operr(e.getMessage()));
        }
    }

    public static void validateTemplate(String content, String params) {
        validateTemplate(content, params, null);
    }

    public static void validateTemplate(String content, String params, String preparams) {
        validateTemplate(content, params, preparams, true);
    }

    public static void validateTemplate(String content, String params, String preparams, boolean decode) {
        CloudFormationDecoder decoder = new CloudFormationDecoder();
        CfnResults result = decoder.decodeFromContent(content, params, preparams, decode);
        validateResults(result);
    }

    public static String getDuration(long duration) {
        if (duration < 1000) {
            return String.valueOf(duration) + "ms";
        } else if (duration < 60000) {
            return String.valueOf(duration / 1000) + "s";
        } else {
            return String.valueOf(duration / 60000) + "m" + String.valueOf((duration % 60000)/1000) + "s";
        }
    }

    public static void attachSystemTagToResources(Object action, Object result, PatternedSystemTag tag) {
        if (!action.getClass().getSimpleName().startsWith("Create") || action instanceof CreateSystemTagAction) {
            return;
        }
        String resourceUuid = getResultStructUuid(result);
        if (StringUtils.isEmpty(resourceUuid)) {
            return;
        }
        SystemTagCreator creator = tag.newSystemTagCreator(resourceUuid);
        creator.inherent = true;
        creator.recreate = false;
        creator.create();
    }

    public static String getResultStructUuid(Object ob) {
        ResultStruct resultStruct = new ResultStruct();
        for (Field field: ob.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object inventory = field.get(ob);

                Field f = field.getType().getField("uuid");
                if (f.get(inventory) == null) {
                    continue;
                }
                f.setAccessible(true);

                resultStruct.setResourceUuid((String)f.get(inventory));
                break;
            } catch (NoSuchFieldException e) {
                logger.warn("no such field: uuid, possibly it is not zstack resource.");
            } catch (IllegalAccessException e) {
                throw new OperationFailureException(operr(e.getMessage()));
            }
        }
        return resultStruct.getResourceUuid();
    }
}
