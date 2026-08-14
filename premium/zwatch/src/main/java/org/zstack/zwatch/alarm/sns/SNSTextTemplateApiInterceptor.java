package org.zstack.zwatch.alarm.sns;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.message.APIMessage;
import org.zstack.sns.SNSConstants;
import org.zstack.zwatch.alarm.sns.template.aliyunsms.APICreateAliyunSmsSNSTextTemplateMsg;
import org.zstack.zwatch.alarm.sns.template.aliyunsms.APIUpdateAliyunSmsSNSTextTemplateMsg;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.zstack.core.Platform.argerr;

@InterceptorForService("alarm.sns")
public class SNSTextTemplateApiInterceptor implements ApiMessageInterceptor {
    static final String PARAM_ALARM_CONDITION = "ALARM_CONDITION";

    @Autowired
    private PluginRegistry pluginRegistry;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateAliyunSmsSNSTextTemplateMsg) {
            validate((APICreateAliyunSmsSNSTextTemplateMsg) msg);
        } else if (msg instanceof APICreateSNSTextTemplateMsg) {
            validate((APICreateSNSTextTemplateMsg) msg);
        } else if (msg instanceof APIUpdateAliyunSmsSNSTextTemplateMsg) {
            validate((APIUpdateAliyunSmsSNSTextTemplateMsg) msg);
        } else if (msg instanceof APIUpdateSNSTextTemplateMsg) {
            validate((APIUpdateSNSTextTemplateMsg) msg);
        }
        return msg;
    }

    private void validate(APICreateAliyunSmsSNSTextTemplateMsg msg) {
        if (!SNSConstants.ALIYUNSMS_PLATFORM.equals(msg.getApplicationPlatformType())) {
            throw new ApiMessageInterceptionException(argerr("invalid application platform type[%s]", msg.getApplicationPlatformType()));
        }

        if (msg.getType() == null) {
            msg.setType(SNSTextTemplateType.COMBINED.toString());
        }

        checkAliyunSmsSignAndCode(msg.getSign(), msg.getAlarmTemplateCode(), msg.getEventTemplateCode());

        List<String> whiteList = new ArrayList<>();
        whiteList.add(PARAM_ALARM_CONDITION);

        List<String> errorAlarmParams = getUnknownParams(msg.getTemplate(), SNSTextTemplateType.ALARM.toString(), whiteList);
        if (!errorAlarmParams.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("parameters:\n %s are not supported by ZStack, available values are:\n %s",
                    String.join(",\n", errorAlarmParams), String.join(",\n" ,AbstractTextTemplate.defaultSupportedParams.get(SNSTextTemplateType.ALARM))));
        }

        List<String> errorEventParams = getUnknownParams(msg.getEventTemplate(), SNSTextTemplateType.EVENT.toString(), whiteList);

        if (!errorEventParams.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("parameters:\n %s are not supported by ZStack, available values are:\n %s",
                    String.join(",\n", errorEventParams), String.join(",\n" ,AbstractTextTemplate.defaultSupportedParams.get(SNSTextTemplateType.EVENT))));
        }
    }

    private void validate(APICreateSNSTextTemplateMsg msg) {
        if (msg.getType() == null) {
            msg.setType(SNSTextTemplateType.ALARM.toString());
        }

        Optional<TextTemplateFactory> opt = pluginRegistry.getExtensionList(TextTemplateFactory.class)
                .stream().filter(tf -> tf.getApplicationPlatformType().equals(msg.getApplicationPlatformType()))
                .findFirst();

        if (!opt.isPresent()) {
            throw new ApiMessageInterceptionException(argerr("invalid application platform type[%s]", msg.getApplicationPlatformType()));
        }

        TextTemplateFactory tf = opt.get();
        if (!tf.isSupportCustomTemplate()) {
            throw new ApiMessageInterceptionException(argerr("application platform/endpoint [%s] doesn't support user-defined template", msg.getApplicationPlatformType()));
        }

        tf.checkTemplate(msg.getTemplate(), msg.getRecoveryTemplate(), msg.getType());


        List<String> errorParams = getUnknownParams(msg.getTemplate(), msg.getType(),null);

        if (msg.getRecoveryTemplate() != null) {
            List<String> errorRecoverParams = getUnknownParams(msg.getRecoveryTemplate(), msg.getType(),null);

            errorParams.addAll(errorRecoverParams);
        }

        if (!errorParams.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("parameters:\n %s are not supported by ZStack, available values are:\n %s",
                    String.join(",\n", errorParams), String.join(",\n" ,AbstractTextTemplate.defaultSupportedParams.get(SNSTextTemplateType.get(msg.getType())))));
        }

    }

    private void checkAliyunSmsSignAndCode(String sign, String alarmTemplateCode, String eventTemplateCode) {
        if (sign!= null && (sign.length() < 2 || sign.length() > 12)) {
            throw new ApiMessageInterceptionException(argerr("The length of aliyun sms sign should between 2 to 12 characters. Got sign: [%s] with [%d] characters.",
                    sign, sign.length()));
        }

        if (alarmTemplateCode != null && alarmTemplateCode.length() != 13) {
            throw new ApiMessageInterceptionException(argerr("Sms template code is a string with 13 characters. Got alarm template code: [%s] with [%d] characters.",
                    alarmTemplateCode, alarmTemplateCode.length()));
        }

        if (eventTemplateCode != null && eventTemplateCode.length() != 13) {
            throw new ApiMessageInterceptionException(argerr("Sms template code is a string with 13 characters. Got event template code: [%s] with [%d] characters.",
                    eventTemplateCode, eventTemplateCode.length()));
        }
    }

    private List<String> getUnknownParams(String template, String type, List whiteList) {
        Pattern pattern = Pattern.compile("\\$\\{(.*?)}");
        Matcher matcher = pattern.matcher(template);
        List<String> errorParams = new ArrayList<>();
        while (matcher.find()) {
            if (matcher.groupCount() == 0) continue;
            String param = matcher.group(1).trim().replace("\n", "");

            Pattern variablePattern = Pattern.compile("(\\w+)");
            Matcher variableMatcher = variablePattern.matcher(param);

            if (!variableMatcher.find()) {
                errorParams.add(param);
                continue;
            }

            String variableParam = variableMatcher.group(1);

            if (StringUtils.isEmpty(variableParam) || !AbstractTextTemplate.defaultSupportedParams.get(SNSTextTemplateType.get(type)).contains(variableParam)) {
                if (whiteList == null || !whiteList.contains(variableParam)) {
                    errorParams.add(param);
                }
            }
        }

        Pattern checkIllegalCharacter = Pattern.compile("\\$(?!\\{)(.*?)\"");
        Matcher matcherIllegalCharacter = checkIllegalCharacter.matcher(template);
        while (matcherIllegalCharacter.find()) {
            errorParams.add(matcherIllegalCharacter.group());
        }

        return errorParams;
    }

    private void validate(APIUpdateAliyunSmsSNSTextTemplateMsg msg) {

        checkAliyunSmsSignAndCode(msg.getSign(), msg.getAlarmTemplateCode(), msg.getEventTemplateCode());

        List<String> errorParams = new ArrayList<>();
        if (msg.getTemplate() != null) {
            List<String> whiteList = new ArrayList<>();
            whiteList.add(PARAM_ALARM_CONDITION);
            List<String> errorAlarmParams = getUnknownParams(msg.getTemplate(), SNSTextTemplateType.ALARM.toString(), whiteList);
            if (!errorAlarmParams.isEmpty()) {
                throw new ApiMessageInterceptionException(argerr("parameters:\n %s are not supported by ZStack, available values are:\n %s",
                        String.join(",\n", errorParams), String.join(",\n", AbstractTextTemplate.defaultSupportedParams.get(SNSTextTemplateType.ALARM))));
            }

        }

        if (msg.getEventTemplate() != null) {
            List<String> errorAlarmParams = getUnknownParams(msg.getEventTemplate(), SNSTextTemplateType.EVENT.toString(), null);
           if (!errorAlarmParams.isEmpty()) {
               throw new ApiMessageInterceptionException(argerr("parameters:\n %s are not supported by ZStack, available values are:\n %s",
                       String.join(",\n", errorParams), String.join(",\n", AbstractTextTemplate.defaultSupportedParams.get(SNSTextTemplateType.EVENT))));
           }
        }

    }

    private void validate(APIUpdateSNSTextTemplateMsg msg) {
        SNSTextTemplateVO vo = dbf.findByUuid(msg.getUuid(), SNSTextTemplateVO.class);
        
        if (msg.getTemplate() != null) {
            List<String> errorParams = getUnknownParams(msg.getTemplate(), vo.getType().toString(), null);

            if (!errorParams.isEmpty()) {
                throw new ApiMessageInterceptionException(argerr("parameters:\n %s are not supported by ZStack, available values are:\n %s",
                        String.join(",\n", errorParams), String.join(",\n" ,AbstractTextTemplate.defaultSupportedParams.get(vo.getType()))));
            }

            Optional<TextTemplateFactory> opt = pluginRegistry.getExtensionList(TextTemplateFactory.class)
                    .stream().filter(tf -> tf.getApplicationPlatformType().equals(vo.getApplicationPlatformType()))
                    .findFirst();

            if (!opt.isPresent()) {
                throw new ApiMessageInterceptionException(argerr("invalid application platform type[%s]", vo.getApplicationPlatformType()));
            }

            TextTemplateFactory tf = opt.get();
            if (!tf.isSupportCustomTemplate()) {
                throw new ApiMessageInterceptionException(argerr("application platform/endpoint [%s] doesn't support user-defined template", vo.getApplicationPlatformType()));
            }

            tf.checkTemplate(msg.getTemplate(), msg.getRecoveryTemplate(), vo.getType().toString());
        }

        if (msg.getRecoveryTemplate() != null) {
            List<String> errorRecoverParams = getUnknownParams(msg.getRecoveryTemplate(), vo.getType().toString(),null);
            if (!errorRecoverParams.isEmpty()) {
                throw new ApiMessageInterceptionException(argerr("parameters:\n %s are not supported by ZStack, available values are:\n %s",
                        String.join(",\n", errorRecoverParams), String.join(",\n" ,AbstractTextTemplate.defaultSupportedParams.get(vo.getType()))));
            }

        }
    }
}
