package org.zstack.cloudformation.template.decoder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.zstack.cloudformation.template.CloudFormationDecoder;
import org.zstack.cloudformation.template.function.TemplateFunctionUtils;
import org.zstack.cloudformation.template.struct.CfnResults;
import org.zstack.cloudformation.template.struct.CloudFormationConstants;
import org.zstack.cloudformation.template.struct.ResourceStruct;
import org.zstack.cloudformation.template.struct.ResourceType;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2018/5/28.
 */
public class ResourceDecoder extends AbstractCfnRootDecoder {
    private static final CLogger logger = Utils.getLogger(ResourceDecoder.class);

    private ResourceStruct getResourceFromResult(CfnResults result, String name) {
        for (ResourceStruct s: result.getResources()) {
            if (s.getResourceName().equals(name)) {
                return s;
            }
        }
        return null;
    }

    private Object decodeValues(JsonElement element, CfnResults result, ResourceStruct resource) {
        if (element.isJsonPrimitive()) {
            return CloudFormationDecoder.getValueByType((JsonPrimitive)element);
        } else if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
            Map<String, Object> params = new HashMap<>();

            for (Map.Entry<String, JsonElement> e: es) {
                if (CloudFormationConstants.functions.contains(e.getKey())) {
                    List<String> refs = TemplateFunctionUtils.getFunctions(e.getKey()).getRefValue(e.getValue(), result);
                    if (refs != null && !refs.isEmpty()) {
                        if (refs.contains(resource.getResourceName())) {
                            throw new OperationFailureException(operr(
                                    "Resource %s cannot depends on itself, please check %s in Resource [%s]",
                                    resource.getResourceName(), e.getKey(), resource.getResourceName()));
                        }
                        resource.getInDegree().addAll(refs);
                    }
                    return TemplateFunctionUtils.getFunctions(e.getKey()).getFunctionResult(e.getValue(), result);
                } else {
                    params.put(e.getKey(), decodeValues(e.getValue(), result, resource));
                }
            }
            return params;
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            List<Object> l = new ArrayList<>();
            for (JsonElement e: array) {
                l.add(decodeValues(e, result, resource));
            }
            return l;
        } else {
            throw new OperationFailureException(operr("Resource value body cannot support null!"));
        }
    }

    private String getOtherAction(String resource) {
        return resource + CloudFormationConstants.actionSuffix;
    }

    private void setResources(String key, Object value, ResourceStruct resource) {
        if (key.equals("Type")) {
            String type = (String)value;
            if (type.contains("::Resource::")) {
                String resourceType = CloudFormationDecoder.decodeResourceType((String)value);
                resource.setAction(DecoderUtils.getCreateAction(resourceType));
                resource.setType(ResourceType.Resource);
                resource.setResourceType(resourceType);
            } else {
                resource.setAction(getOtherAction(CloudFormationDecoder.decodeResourceType((String)value)));
                resource.setType(ResourceType.Action);
            }
        } else if (key.equals("DeletionPolicy")) {
            resource.setDeletePolicy((String)value);
        } else if (key.equals("Description")) {
            resource.setDescription((String)value);
        } else if (key.equals("DependsOn")) {
            if (value instanceof List) {
                resource.getInDegree().addAll((List)value);
            } else {
                resource.getInDegree().add((String)value);
            }
        } else if (key.equals("Properties")) {
            resource.setProperties((Map)value);
        } else if (key.equals("MockFailed")) {
            resource.setMockFailed((Boolean) value);
        } else {
            throw new OperationFailureException(operr(String.format("unsupported resourceKeys: %s", key)));
        }
    }

    private void decodeResources(JsonElement element, CfnResults result, String name) {
        if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
            ResourceStruct p = getResourceFromResult(result, name);
            if (p == null) {
                throw new OperationFailureException(operr("resourceName must be found in result, or it is invalid cfn json."));
            }
            for (Map.Entry<String, JsonElement> e: es) {
                if (CloudFormationConstants.resourceKeys.contains(e.getKey())) {
                    Object o = decodeValues(e.getValue(), result, p);
                    setResources(e.getKey(), o, p);
                } else {
                    throw new OperationFailureException(operr(String.format("unsupported key [%s] in Resources [%s]", e.getKey(), name)));
                }
            }
        } else {
            throw new OperationFailureException(operr("Parameters body cannot support null!"));
        }
    }

    @Override
    public void decode(JsonElement element, CfnResults result) {
//        CloudFormationDecoder.printTree(element);
        if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
            /**
             * scan twice, we put all resourceName in result first, then decode it
             */
            es.forEach(e -> {
                ResourceStruct p = new ResourceStruct();
                p.setResourceName(e.getKey());
                result.getResources().add(p);
            });

            es.forEach(e -> {
                decodeResources(e.getValue(), result, e.getKey());
            });
        } else {
            throw new OperationFailureException(operr("Mappings root body must be json object!"));
        }
    }

    private List<String> findRef(JsonElement element) {
        List<String> ref = new ArrayList<>();
        Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
        for (Map.Entry<String, JsonElement> e: es) {
            if (e.getKey().equals("Ref")) {
                String value = e.getValue().getAsString();
                if (!value.startsWith("ZStack::")) {
                    ref.add(value);
                }
            } else {
                ref.addAll(findRefs(e.getValue()));
            }
        }

        return ref;
    }

    private List<String> findRefs(JsonElement element) {
        if (element.isJsonPrimitive() || element.isJsonNull()) {
            return CollectionDSL.list();
        }
        List<String> refs = new ArrayList<>();
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement e: array) {
                if (e.isJsonObject() || e.isJsonArray()) {
                    refs.addAll(findRefs(e));
                }
            }
        } else {
            refs.addAll(findRef(element));
        }
        return refs;
    }

    private Map<String, String> getRefParameters(JsonElement element, String msg) {
        Map<String, String> r = new HashMap<>();

        if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
            for (Map.Entry<String, JsonElement> e: es) {
                String resourceType = DecoderUtils.getResourceTypeInMsg(e.getKey(), msg);
                if (resourceType == null) {
                    continue;
                }
                List<String> refs = findRefs(e.getValue());
                refs.forEach(ref -> {
                    r.put(ref, resourceType);
                });
            }
        }
        return r;
    }

    private Map<String, String> getParameters(JsonElement element) {
        Map<String, String> r = new HashMap<>();
        if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
            String msg = null;
            for (Map.Entry<String, JsonElement> e: es) {
                Set<Map.Entry<String, JsonElement>> params = e.getValue().getAsJsonObject().entrySet();
                for (Map.Entry<String, JsonElement> p: params) {
                    if (p.getKey().equals("Type")) {
                        if (!p.getValue().isJsonPrimitive()) {
                            throw new OperationFailureException(operr("Resource Type must be String!"));
                        }
                        if (!((JsonPrimitive)p.getValue()).isString()) {
                            throw new OperationFailureException(operr("Resource Type must be String!"));
                        }
                        msg = DecoderUtils.getMsgFromResourceType(CloudFormationDecoder.decodeResourceType(p.getValue().getAsString()),
                                CloudFormationDecoder.decodeActionType(p.getValue().getAsString()));
                        break;
                    }
                }
                for (Map.Entry<String, JsonElement> p: params) {
                    if (msg == null) {
                        continue;
                    }
                    if (p.getKey().equals("Properties")) {
                        Map<String, String> tmp = getRefParameters(p.getValue(), msg);
                        if (tmp != null) {
                            r.putAll(tmp);
                        }
                        break;
                    }
                }
                msg = null;
            }
        } else {
            throw new OperationFailureException(operr("Resource root body must be json object!"));
        }
        return r;
    }

    public Map<String, String> getResourceParametersType(JsonElement element) {
        Map<String, String> params = new HashMap<>();
        if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
            for (Map.Entry<String, JsonElement> e: es) {
                if (e.getKey().equals("Resources")) {
                    params = getParameters(e.getValue());
                    break;
                }
            }
        } else {
            throw new OperationFailureException(operr("Resource root body must be json object!"));
        }
        return params;
    }
}
