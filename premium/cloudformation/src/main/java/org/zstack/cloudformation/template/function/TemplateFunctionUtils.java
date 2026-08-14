package org.zstack.cloudformation.template.function;

import com.google.gson.JsonElement;
import org.zstack.cloudformation.template.struct.CfnResults;
import org.zstack.cloudformation.template.struct.CloudFormationConstants;
import org.zstack.cloudformation.template.struct.MappingStruct;
import org.zstack.cloudformation.template.struct.ResourceStruct;
import org.zstack.header.errorcode.OperationFailureException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2018/5/30.
 */
public class TemplateFunctionUtils {
    public static TemplateFunction getFunctions(String f) {
        if (f.equals("Ref")) {
            return new RefTemplateFunction();
        } else {
            String func = f.substring(CloudFormationConstants.funcPrefix.length());
            String clz = CloudFormationConstants.funcPackage + func + CloudFormationConstants.funcSuffix;
            try {
                return (TemplateFunction)Class.forName(clz).newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                e.printStackTrace();
                throw new OperationFailureException(operr(String.format("not supported functions [%s] in zstack resource template!", f)));
            }
        }
    }

    private static String getStringFromMappings(String resource, CfnResults result) {
        for (MappingStruct s: result.getMappings()) {
            if (s.getMappingName().equals(resource)) {
                return resource;
            }
        }
        return null;
    }

    private static String getStringFromResources(String resource, CfnResults result) {
        for (ResourceStruct s: result.getResources()) {
            if (s.getResourceName().equals(resource)) {
                return resource;
            }
        }
        return null;
    }

    public static String getString(String resource, CfnResults result) {
        String r = getStringFromMappings(resource, result);
        if (r == null) {
            r = getStringFromResources(resource, result);
        }
        if (r == null) {
            throw new OperationFailureException(operr(String.format("No Ref [%s] found!", resource)));
        }
        return r;
    }

    public static Object getObject(JsonElement element, CfnResults result) {
        return getObject(element, result, false);
    }

    public static Object getObject(JsonElement element, CfnResults result, boolean skip) {
        Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
        for (Map.Entry<String, JsonElement> e: es) {
            if (CloudFormationConstants.functions.contains(e.getKey())) {
                Object o =  TemplateFunctionUtils.getFunctions(e.getKey()).getFunctionResult(e.getValue(), result);
                return o;
            } else if (!skip) {
                throw new OperationFailureException(operr("only functions can in Function, but found %s", e.getKey()));
            }
        }
        throw new OperationFailureException(operr("element is null!"));
    }

    public static String getString(JsonElement element, CfnResults result, boolean skip) {
        Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
        for (Map.Entry<String, JsonElement> e: es) {
            if (CloudFormationConstants.functions.contains(e.getKey())) {
                return TemplateFunctionUtils.getFunctions(e.getKey()).getFunctionResult(e.getValue(), result).toString();
            } else if (!skip) {
                throw new OperationFailureException(operr(String.format("only functions can in Function, but found %s", e.getKey())));
            }
        }
        throw new OperationFailureException(operr("element is null!"));
    }

    public static List<String> getRef(JsonElement element, CfnResults result) {
        Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
        for (Map.Entry<String, JsonElement> e: es) {
            if (CloudFormationConstants.functions.contains(e.getKey())) {
                return TemplateFunctionUtils.getFunctions(e.getKey()).getRefValue(e.getValue(), result);
            }
        }
        return null;
    }
}
