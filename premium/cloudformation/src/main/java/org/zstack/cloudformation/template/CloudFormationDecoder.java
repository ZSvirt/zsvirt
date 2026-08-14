package org.zstack.cloudformation.template;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.cloudformation.template.decoder.CfnRootDecoder;
import org.zstack.cloudformation.template.decoder.ConditionDecoder;
import org.zstack.cloudformation.template.decoder.DescriptionDecoder;
import org.zstack.cloudformation.template.decoder.MappingDecoder;
import org.zstack.cloudformation.template.decoder.OutputDecoder;
import org.zstack.cloudformation.template.decoder.ParameterDecoder;
import org.zstack.cloudformation.template.decoder.PreParameterDecoder;
import org.zstack.cloudformation.template.decoder.ResourceDecoder;
import org.zstack.cloudformation.template.decoder.TemplateVersionDecoder;
import org.zstack.cloudformation.template.struct.CfnResults;
import org.zstack.cloudformation.template.struct.CloudFormationConstants;
import org.zstack.cloudformation.template.struct.ParameterStruct;
import org.zstack.cloudformation.template.struct.PreParameterStruct;
import static org.zstack.core.Platform.operr;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.cloudformation.CloudformationDecoderExtensionPoint;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by mingjian.deng on 2018/5/28.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CloudFormationDecoder {
    private static final CLogger logger = Utils.getLogger(CloudFormationDecoder.class);

    @Autowired
    private PluginRegistry pluginRgty;

    private CfnResults result = new CfnResults();
    private Map<String, JsonElement> p = new HashMap<>();

    public CfnResults getResult() { 
        return result;
    }

    public void setResult(CfnResults result) {
        this.result = result;
    }

    public static Object getValueByType(JsonPrimitive e) {
        if (e.isNumber()) {
            return e.getAsNumber();
        } else if (e.isBoolean()) {
            return e.getAsBoolean();
        } else {
            return e.getAsString();
        }
    }

    public static String decodeResourceType(String value) {
        String[] s = value.split(CloudFormationConstants.split);
        if (s.length != 3) {
            throw new OperationFailureException(operr(String.format("invalid Type format [%s]", value)));
        }
        if (!s[0].equals("ZStack") && !s[0].equals("Cloud")) {
            throw new OperationFailureException(operr(String.format("only support Cloud resources, but get [%s]", s[0])));
        }
        if (s[1].equals("Resource")) {
            if (!CloudFormationConstants.supportedResources.contains(s[2])) {
                throw new OperationFailureException(operr(String.format("cannot support such resource type: %s", s[2])));
            }
        } else if (s[1].equals("Action")) {
            if (!CloudFormationConstants.supportedActions.contains(s[2])) {
                throw new OperationFailureException(operr(String.format("cannot support such action type: %s", s[2])));
            }
        } else {
            throw new OperationFailureException(operr(String.format("only support Resource or Action, but get [%s]", s[1])));
        }

        return s[2];
    }

    public static String decodeActionType(String value) {
        String[] s = value.split(CloudFormationConstants.split);
        if (s.length != 3) {
            throw new OperationFailureException(operr(String.format("invalid Type format [%s]", value)));
        }
        if (!s[0].equals("ZStack") && !s[0].equals("Cloud")) {
            throw new OperationFailureException(operr(String.format("only support Cloud resources, but get [%s]", s[0])));
        }
        if (!s[1].equals("Resource") && !s[1].equals("Action")) {
            throw new OperationFailureException(operr(String.format("only support Resource or Action, but get [%s]", s[1])));
        }

        return s[1];
    }

    public static String doConvert(String str) {
        if (str != null && !str.equals("")) {
            str = str.substring(0, 1).toLowerCase() + str.substring(1);
        }
        return str;
    }

    public static Object getValueByType(String type, JsonElement element) {
        if (type.equals("Json")) {
            if (!element.isJsonObject() && !element.isJsonArray()) {
                throw new OperationFailureException(operr(String.format(
                        "Default value must be Json format which described by Type [%s], but [%s] was found", type, element.toString())));
            }
            return element.toString();
        } else if (!element.isJsonPrimitive()) {
            // only support JsonPrimitive value
            throw new OperationFailureException(operr(String.format("invalid type: %s", type)));
        } else {
            JsonPrimitive e = (JsonPrimitive)element;
            if (type.equals("Number")) {
                if (e.isNumber()) {
                    return element.getAsNumber();
                }
            } else if (type.equals("Boolean")) {
                if (e.isBoolean()) {
                    return element.getAsBoolean();
                }
            } else if (type.equals("CommaDelimitedList")) {
                if (e.isString()) {
                    return CollectionDSL.list(element.getAsString().split(","));
                }
            } else if (type.equals("String")) {
                if (e.isString()) {
                    return element.getAsString();
                }
            } else {
                throw new OperationFailureException(operr(String.format("invalid type: %s", type)));
            }
            throw new OperationFailureException(operr(String.format("type is: %s, but get value: %s", type, e.getAsString())));
        }
    }

    public static void printTree(JsonElement element) {
        if (element.isJsonPrimitive()) {
            logger.debug("--------------------");
            logger.debug(element.getAsString());
            return;
        }

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (array == null) {
                return;
            }
            for (JsonElement e: array) {
                printTree(e);
            }
            return;
        }

        if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
            es.forEach(e -> {
                printTree(e.getValue());
            });
        }
    }

    private CfnRootDecoder getDecoderExt(String type) {
        CfnRootDecoder decoder = null;
        boolean found = false;
        for (CloudformationDecoderExtensionPoint exp: pluginRgty.getExtensionList(CloudformationDecoderExtensionPoint.class)) {
            decoder = exp.getDecoderFromString(type);
            if (decoder != null) {
                DebugUtils.Assert(!found, String.format("find two CloudformationDecoderExtensionPoints to decode this type[%s]", type));
            }
        }
        return decoder;
    }

    class DecoderPeer {
        CfnRootDecoder decoder;
        JsonElement e;

        public DecoderPeer(CfnRootDecoder decoder, JsonElement e) {
            this.decoder = decoder;
            this.e = e;
        }
    }

    private void decode(JsonElement element) {
        DebugUtils.Assert(element != null, "cannot decode a null element!");
        if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> root = element.getAsJsonObject().entrySet();
            List<DecoderPeer> decoders = new ArrayList<>();
            root.forEach(e -> {
                CfnRootDecoder decoder;
                switch (e.getKey()) {
                    case "ZStackTemplateFormatVersion" :
                        decoder = new TemplateVersionDecoder();
                        break;
                    case "Description" :
                        decoder = new DescriptionDecoder();
                        break;
                    case "Parameters" :
                        decoder = new ParameterDecoder();
                        decoder.setWeight(10);
                        break;
                    case "Resources" :
                        decoder = new ResourceDecoder();
                        break;
                    case "Outputs" :
                        decoder = new OutputDecoder();
                        decoder.setWeight(90);
                        break;
                    case "Conditions" :
                        decoder = new ConditionDecoder();
                        decoder.setWeight(20);
                        break;
                    case "Mappings" :
                        decoder = new MappingDecoder();
                        decoder.setWeight(15);
                        break;
                    case "Pre-Parameters" :
                        decoder = new PreParameterDecoder();
                        decoder.setWeight(5);
                        break;
                    default:
                        decoder = null;
                }
                if (decoder == null) {
                    decoder = getDecoderExt(e.getKey());
                    if (decoder == null) {
                        throw new OperationFailureException(operr(String.format("invalid decoder: %s!", e.getKey())));
                    }
                }
                decoders.add(new DecoderPeer(decoder, e.getValue()));
            });
            decoders.sort((d1, d2) -> d1.decoder.getWeight() - d2.decoder.getWeight());

            decoders.forEach(d -> {
                d.decoder.decode(d.e, result);
            });

        } else {
            throw new OperationFailureException(operr("no root element found, please check your cfn formation!"));
        }
    }

    private void parsePreParams(JsonElement element) {
        if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> root = element.getAsJsonObject().entrySet();
            for (Map.Entry<String, JsonElement> e: root) {
                if (e.getKey().equals("Pre-Parameters")) {
                    CfnRootDecoder decoder = new PreParameterDecoder();
                    decoder.decode(e.getValue(), result);
                }
            }
        }

        for (PreParameterStruct param: result.getPreparams()) {
            if (param.getValue() == null) {
                if (p.get(param.getParamName()) != null) {
                    JsonElement value = p.get(param.getParamName());
                    param.setValue(getValueByType(param.getType(), value));
                } else if (param.getDefaultValue() != null) {
                    param.setValue(param.getDefaultValue());
                } else {
                    result.getRequiredParams().add(param.getParamName());
                }
            }
        }
    }

    private void parseParams(JsonElement element) {
        if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> root = element.getAsJsonObject().entrySet();
            for (Map.Entry<String, JsonElement> e: root) {
                if (e.getKey().equals("Parameters")) {
                    CfnRootDecoder decoder = new ParameterDecoder();
                    decoder.decode(e.getValue(), result);
                }
            }
        }

        for (ParameterStruct param: result.getParams()) {
            if (param.getValue() == null) {
                if (p.get(param.getParamName()) != null) {
                    JsonElement value = p.get(param.getParamName());
                    param.setValue(getValueByType(param.getType(), value));
                } else if (param.getDefaultValue() != null) {
                    param.setValue(param.getDefaultValue());
                } else {
                    result.getRequiredParams().add(param.getParamName());
                }
            }
        }
    }

    private void parseMappings(JsonElement element) {
        if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> root = element.getAsJsonObject().entrySet();
            for (Map.Entry<String, JsonElement> e: root) {
                if (e.getKey().equals("Mappings")) {
                    CfnRootDecoder decoder = new MappingDecoder();
                    decoder.decode(e.getValue(), result);
                }
            }
        }
    }

    private void parseConditons(JsonElement element) {
        if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> root = element.getAsJsonObject().entrySet();
            for (Map.Entry<String, JsonElement> e: root) {
                if (e.getKey().equals("Conditions")) {
                    CfnRootDecoder decoder = new ConditionDecoder();
                    decoder.decode(e.getValue(), result);
                }
            }
        }
    }

    private void parseVersionAndDescription(JsonElement element) {
        if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> root = element.getAsJsonObject().entrySet();
            for (Map.Entry<String, JsonElement> e: root) {
                if (e.getKey().equals("ZStackTemplateFormatVersion")) {
                    CfnRootDecoder decoder = new TemplateVersionDecoder();
                    decoder.decode(e.getValue(), result);
                } else if (e.getKey().equals("Description")) {
                    CfnRootDecoder decoder = new DescriptionDecoder();
                    decoder.decode(e.getValue(), result);
                }
            }
        }
    }

    private void getParams(JsonElement element) {
        if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> param = element.getAsJsonObject().entrySet();
            param.forEach(e -> {
                if (e.getValue().isJsonPrimitive()) {
                    p.put(e.getKey(), e.getValue());
                }
            });
        }
    }

    private void preParse(JsonElement element, JsonElement params, JsonElement preparams) {
        /**
         * parse params first, then parse mappings
         */
        if (params != null) {
            getParams(params);
        }
        if (preparams != null) {
            getParams(preparams);
        }

        parseParams(element);
        parsePreParams(element);

        parseMappings(element);
        preParse(element);
        parseConditons(element);

        mergePreParams();
    }

    private void preParse(JsonElement element) {
        /**
         * parse version and params
         */
        parseVersionAndDescription(element);
        parseParams(element);
        parsePreParams(element);
    }

    private boolean isRedundantParams(ParameterStruct params) {
        for (ParameterStruct p: result.getParams()) {
            if (p.getParamName().equals(params.getParamName())) {
                return true;
            }
        }
        return false;
    }

    private void mergePreParams() {
        for (ParameterStruct struct: result.getPreparams()) {
            if (isRedundantParams(struct)) {
                logger.warn(String.format("pre-params: %s is a redundant params", struct.print()));
                continue;
            }
            logger.debug(String.format("merge pre-params: %s to params", struct.print()));
            result.getParams().add(struct);
        }
    }

    public CfnResults decodeFromContent(String content, String paramsContent) {
        return decodeFromContent(content, paramsContent, true);
    }

    public CfnResults decodeFromContent(String content, String paramsContent, boolean decode) {
        return decodeFromContent(content, paramsContent, null, decode);
    }

    public CfnResults decodeFromContent(String content, String paramsContent, String preparamsContent, boolean decode) {
        JsonParser p = new JsonParser();
        try {
            JsonElement element = p.parse(content);

            JsonElement params = paramsContent == null ? null : p.parse(paramsContent);
            JsonElement preparams = preparamsContent == null ? null : p.parse(preparamsContent);
            if (params == null && preparams == null) {
                preParse(element);
            } else {
                preParse(element, params, preparams);
            }

            if (decode) {
                decode(element);
            }
        } catch (JsonSyntaxException e) {
            throw new OperationFailureException(operr("Wrong json format, causes: %s", e.getMessage()));
        }
        return result;
    }
}
