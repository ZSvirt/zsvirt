package org.zstack.sns;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.message.APIMessage;
import org.zstack.header.rest.RESTFacade;
import org.zstack.sns.platform.dingtalk.*;
import org.zstack.sns.platform.email.*;
import org.zstack.sns.platform.feishu.*;
import org.zstack.sns.platform.http.APICreateSNSHttpEndpointMsg;
import org.zstack.sns.platform.http.APISNSHttpTestConnectionMsg;
import org.zstack.sns.platform.microsoftteams.APICreateSNSMicrosoftTeamsEndpointMsg;
import org.zstack.sns.platform.microsoftteams.APISNSMicrosoftTeamsTestConnectionMsg;
import org.zstack.sns.platform.microsoftteams.APIUpdateSNSMicrosoftTeamsEndpointMsg;
import org.zstack.sns.platform.snmp.APICreateSNSSnmpPlatformMsg;
import org.zstack.sns.platform.snmp.APISNSSnmpTestConnectionMsg;
import org.zstack.sns.platform.snmp.SNSSnmpPlatformVO;
import org.zstack.sns.platform.snmp.SNSSnmpPlatformVO_;
import org.zstack.sns.platform.wecom.*;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;

@InterceptorForService("sns")
public class SNSApiInterceptor implements ApiMessageInterceptor {
    private static Pattern dingdingPhoneNumberFormat = Pattern.compile("^\\+\\d{2,3}-\\d{11}$");
    private static Pattern SmsPhoneNumberFormat = Pattern.compile("^\\+\\d{1,6}-\\d{6,14}$");
    private static Pattern ipFormat = Pattern.compile(".*?\\.[0-9]+");

    @Autowired
    private RESTFacade restf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateSNSEmailEndpointMsg) {
            validate((APICreateSNSEmailEndpointMsg) msg);
        } else if (msg instanceof APIUpdateEmailAddressOfSNSEmailEndpointMsg) {
            validate((APIUpdateEmailAddressOfSNSEmailEndpointMsg) msg);
        } else if (msg instanceof APICreateSNSHttpEndpointMsg) {
            validate((APICreateSNSHttpEndpointMsg) msg);
        } else if (msg instanceof APICreateSNSDingTalkEndpointMsg) {
            validate((APICreateSNSDingTalkEndpointMsg) msg);
        } else if (msg instanceof APIAddSNSDingTalkAtPersonMsg) {
            validate((APIAddSNSDingTalkAtPersonMsg) msg);
        } else if (msg instanceof APIAddEmailAddressToSNSEmailEndpointMsg) {
            validate((APIAddEmailAddressToSNSEmailEndpointMsg) msg);
        } else if (msg instanceof APIAddSNSSmsReceiverMsg) {
            validate((APIAddSNSSmsReceiverMsg) msg);
        } else if (msg instanceof APICreateSNSMicrosoftTeamsEndpointMsg) {
            validate((APICreateSNSMicrosoftTeamsEndpointMsg) msg);
        } else if (msg instanceof APICreateSNSSnmpPlatformMsg) {
            validate((APICreateSNSSnmpPlatformMsg) msg);
        } else if (msg instanceof APICreateSNSFeiShuEndpointMsg) {
            validate((APICreateSNSFeiShuEndpointMsg) msg);
        } else if (msg instanceof APICreateSNSWeComEndpointMsg) {
            validate((APICreateSNSWeComEndpointMsg) msg);
        } else if (msg instanceof APIUpdateSNSFeiShuEndpointMsg) {
            validate((APIUpdateSNSFeiShuEndpointMsg) msg);
        } else if (msg instanceof APIUpdateSNSWeComEndpointMsg) {
            validate((APIUpdateSNSWeComEndpointMsg) msg);
        } else if (msg instanceof APIUpdateAtPersonOfAtDingTalkEndpointMsg) {
            validate((APIUpdateAtPersonOfAtDingTalkEndpointMsg) msg);
        } else if (msg instanceof APIUpdateAtPersonOfAtFeiShuEndpointMsg) {
            validate((APIUpdateAtPersonOfAtFeiShuEndpointMsg) msg);
        } else if (msg instanceof APIUpdateAtPersonOfAtWeComEndpointMsg) {
            validate((APIUpdateAtPersonOfAtWeComEndpointMsg) msg);
        } else if (msg instanceof APIUpdateSNSDingTalkEndpointMsg) {
            validate((APIUpdateSNSDingTalkEndpointMsg) msg);
        } else if (msg instanceof APIUpdateSNSMicrosoftTeamsEndpointMsg) {
            validate((APIUpdateSNSMicrosoftTeamsEndpointMsg) msg);
        } else if (msg instanceof APISNSDingTalkTestConnectionMsg) {
            validate((APISNSDingTalkTestConnectionMsg) msg);
        } else if (msg instanceof APISNSWeComTestConnectionMsg) {
            validate((APISNSWeComTestConnectionMsg) msg);
        } else if (msg instanceof APISNSFeiShuTestConnectionMsg) {
            validate((APISNSFeiShuTestConnectionMsg) msg);
        } else if (msg instanceof APISNSMicrosoftTeamsTestConnectionMsg) {
            validate((APISNSMicrosoftTeamsTestConnectionMsg) msg);
        } else if (msg instanceof APIAddSNSFeiShuAtPersonMsg) {
            validate((APIAddSNSFeiShuAtPersonMsg) msg);
        } else if (msg instanceof APIAddSNSWeComAtPersonMsg) {
            validate((APIAddSNSWeComAtPersonMsg) msg);
        } else if (msg instanceof APISNSEmailTestConnectionMsg) {
            validate((APISNSEmailTestConnectionMsg) msg);
        } else if (msg instanceof APISNSHttpTestConnectionMsg) {
            validate((APISNSHttpTestConnectionMsg) msg);
        } else if (msg instanceof APIValidateSNSEmailPlatformMsg) {
            validate((APIValidateSNSEmailPlatformMsg) msg);
        } else if (msg instanceof APISNSSnmpTestConnectionMsg) {
            validate((APISNSSnmpTestConnectionMsg) msg);
        }
        return msg;
    }

    private void validate(APISNSSnmpTestConnectionMsg msg) {
        if (StringUtils.isBlank(msg.getPlatformUuid())
                && StringUtils.isBlank(msg.getEndpointUuid())) {
            throw new ApiMessageInterceptionException(argerr("platformUuid and endpointUuid cannot be empty together"));
        }
    }

    private void validate(APIValidateSNSEmailPlatformMsg msg) {
        boolean uuidIsNotNull = StringUtils.isNotBlank(msg.getUuid())
                && !msg.getUuid().equalsIgnoreCase("null");

        if (uuidIsNotNull && !Q.New(SNSEmailPlatformVO.class)
                .eq(SNSEmailPlatformVO_.uuid, msg.getUuid())
                .isExists()) {
            throw new ApiMessageInterceptionException(argerr("uuid [%s] already exists", msg.getUuid()));
        }

        if (!uuidIsNotNull) {
            if (StringUtils.isBlank(msg.getSmtpServer())) {
                throw new ApiMessageInterceptionException(argerr("smtpServer cannot null"));
            }
            if (msg.getSmtpPort() == null) {
                throw new ApiMessageInterceptionException(argerr("smtpPort cannot null"));
            }
            if (StringUtils.isBlank(msg.getUsername())) {
                throw new ApiMessageInterceptionException(argerr("username cannot null"));
            }
            if (StringUtils.isBlank(msg.getPassword())) {
                throw new ApiMessageInterceptionException(argerr("password cannot null"));
            }
        }
    }


    private void validate(APISNSEmailTestConnectionMsg msg) {
        if (!msg.getEmails().isEmpty()) {
            EmailValidator validator = EmailValidator.getInstance(true);
            for (String email : msg.getEmails()) {
                if (!validator.isValid(email)) {
                    throw new ApiMessageInterceptionException(argerr("invalid email address[%s]", email));
                }
            }
        }
        if (StringUtils.isBlank(msg.getPlatformUuid())
                && StringUtils.isBlank(msg.getEndpointUuid())) {
            throw new ApiMessageInterceptionException(argerr("platformUuid and endpointUuid cannot be empty together"));
        }
    }

    private void validate(APISNSHttpTestConnectionMsg msg) {
        if (StringUtils.isNotBlank(msg.getUrl())) {
            validateURL(msg.getUrl());
        }
    }

    private void validate(APIAddSNSWeComAtPersonMsg msg) {
        if (Q.New(SNSWeComAtPersonVO.class).eq(SNSWeComAtPersonVO_.endpointUuid, msg.getEndpointUuid())
                .eq(SNSWeComAtPersonVO_.userId, msg.getUserId())
                .isExists())
            throw new ApiMessageInterceptionException(argerr("userId [%s] already exists", msg.getUserId()));
    }

    private void validate(APIAddSNSFeiShuAtPersonMsg msg) {
        if (Q.New(SNSFeiShuAtPersonVO.class).eq(SNSFeiShuAtPersonVO_.endpointUuid, msg.getEndpointUuid())
                .eq(SNSFeiShuAtPersonVO_.userId, msg.getUserId())
                .isExists())
            throw new ApiMessageInterceptionException(argerr("userId [%s] already exists", msg.getUserId()));
    }

    private void validate(APISNSMicrosoftTeamsTestConnectionMsg msg) {
        Predicate<APISNSMicrosoftTeamsTestConnectionMsg> condition1
                = m -> StringUtils.isBlank(m.getUrl()) && StringUtils.isBlank(m.getTestMsg());
        Predicate<APISNSMicrosoftTeamsTestConnectionMsg> condition2
                = m -> StringUtils.isBlank(m.getEndpointUuid());

        if (condition1.test(msg) && condition2.test(msg)) {
            throw new ApiMessageInterceptionException(argerr("cannot test connection, Because all parameters is null"));
        }

        if (StringUtils.isNotBlank(msg.getUrl())) {
            validateURL(msg.getUrl());
        }
    }

    private void validate(APISNSFeiShuTestConnectionMsg msg) {
        Predicate<APISNSFeiShuTestConnectionMsg> condition1
                = m -> StringUtils.isBlank(m.getUrl()) && StringUtils.isBlank(m.getTestMsg()) && StringUtils.isBlank(m.getSecret());
        Predicate<APISNSFeiShuTestConnectionMsg> condition2
                = m -> StringUtils.isBlank(m.getEndpointUuid());

        if (condition1.test(msg) && condition2.test(msg)) {
            throw new ApiMessageInterceptionException(argerr("cannot test connection, Because all parameters is null"));
        }

        if (StringUtils.isNotBlank(msg.getUrl())) {
            validateURL(msg.getUrl());
        }
    }

    private void validate(APISNSWeComTestConnectionMsg msg) {
        Predicate<APISNSWeComTestConnectionMsg> condition1
                = m -> StringUtils.isBlank(m.getUrl()) && StringUtils.isBlank(m.getTestMsg());
        Predicate<APISNSWeComTestConnectionMsg> condition2
                = m -> StringUtils.isBlank(m.getEndpointUuid());

        if (condition1.test(msg) && condition2.test(msg)) {
            throw new ApiMessageInterceptionException(argerr("cannot test connection, Because all parameters is null"));
        }

        if (StringUtils.isNotBlank(msg.getUrl())) {
            validateURL(msg.getUrl());
        }
    }

    private void validate(APISNSDingTalkTestConnectionMsg msg) {
        Predicate<APISNSDingTalkTestConnectionMsg> condition1
                = m -> StringUtils.isBlank(m.getUrl()) && StringUtils.isBlank(m.getTestMsg()) && StringUtils.isBlank(m.getSecret());
        Predicate<APISNSDingTalkTestConnectionMsg> condition2
                = m -> StringUtils.isBlank(m.getEndpointUuid());

        if (condition1.test(msg) && condition2.test(msg)) {
            throw new ApiMessageInterceptionException(argerr("cannot test connection, Because all parameters is null"));
        }

        if (StringUtils.isNotBlank(msg.getUrl())) {
            validateURL(msg.getUrl());
        }
    }

    private void validate(APIUpdateSNSMicrosoftTeamsEndpointMsg msg) {
        if (StringUtils.isNotBlank(msg.getUrl())) {
            validateURL(msg.getUrl());
        }
    }

    private void validate(APIUpdateSNSDingTalkEndpointMsg msg) {
        if (StringUtils.isNotBlank(msg.getUrl()))
            validateURL(msg.getUrl());
    }

    private void validate(APICreateSNSSnmpPlatformMsg msg) {
        if (Q.New(SNSSnmpPlatformVO.class)
                .eq(SNSSnmpPlatformVO_.snmpAddress, msg.getSnmpAddress())
                .eq(SNSSnmpPlatformVO_.snmpPort, msg.getSnmpPort()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("can not create snmp platform with same address[%s:%s]", msg.getSnmpAddress(), msg.getSnmpPort()));
        }
    }

    private void validate(APICreateSNSMicrosoftTeamsEndpointMsg msg) {
        validateURL(msg.getUrl());
    }

    private void validate(APIAddEmailAddressToSNSEmailEndpointMsg msg) {
        if (Q.New(SNSEmailAddressVO.class)
                .eq(SNSEmailAddressVO_.endpointUuid, msg.getApplicationEndpointUuid())
                .eq(SNSEmailAddressVO_.emailAddress, msg.getEmailAddress()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("can not add same email address to endpoint[uuid:%s]", msg.getApplicationEndpointUuid()));
        }

        EmailValidator validator = EmailValidator.getInstance(true);
        if (!validator.isValid(msg.getEmailAddress())) {
            throw new ApiMessageInterceptionException(argerr("invalid email address[%s]", msg.getEmailAddress()));
        }
    }

    private void validate(APIUpdateEmailAddressOfSNSEmailEndpointMsg msg) {
        if (Q.New(SNSEmailAddressVO.class)
                .eq(SNSEmailAddressVO_.endpointUuid, msg.getApplicationEndpointUuid())
                .eq(SNSEmailAddressVO_.emailAddress, msg.getEmailAddress()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("cannot update email address to %s, which is already exists in endpoint[uuid:%s]", msg.getEmailAddress(), msg.getApplicationEndpointUuid()));
        }

        EmailValidator validator = EmailValidator.getInstance(true);
        if (!validator.isValid(msg.getEmailAddress())) {
            throw new ApiMessageInterceptionException(argerr("invalid email address[%s]", msg.getEmailAddress()));
        }
    }

    private void validate(APIAddSNSSmsReceiverMsg msg) {
        errorOnSmsPhoneNumber(msg.getPhoneNumber());

        if (Q.New(SNSSmsReceiverVO.class).eq(SNSSmsReceiverVO_.endpointUuid, msg.getEndpointUuid())
                .eq(SNSSmsReceiverVO_.phoneNumber, msg.getPhoneNumber()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("phone number [%s] already exists", msg.getPhoneNumber()));
        }
    }

    private void errorOnSmsPhoneNumber(String number) {
        if (!SmsPhoneNumberFormat.matcher(number).matches()) {
            throw new ApiMessageInterceptionException(argerr("invalid phone number[%s], sms number is like +86-18654321234", number));
        }
    }

    private void validate(APIAddSNSDingTalkAtPersonMsg msg) {
        errorOnWrongDingDingPhoneNumber(msg.getPhoneNumber());

        if (Q.New(SNSDingTalkAtPersonVO.class).eq(SNSDingTalkAtPersonVO_.endpointUuid, msg.getEndpointUuid())
                .eq(SNSDingTalkAtPersonVO_.phoneNumber, msg.getPhoneNumber()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("phone number[%s] already exists", msg.getPhoneNumber()));
        }
    }

    private void validateURL(String url) {
        try {
            URL result = new URL(url);
            validateIp(result.getHost());
        } catch (MalformedURLException e) {
            throw new ApiMessageInterceptionException(argerr("invalid url[%s]", url));
        }
    }

    private void validateIp(String host) {
        if (ipFormat.matcher(host).matches() && !NetworkUtils.isIpv4Address(host) && !IPv6NetworkUtils.isIpv6Address(host)) {
            throw new ApiMessageInterceptionException(argerr("[%s] is not a legal ip", host));
        }
    }

    private void errorOnWrongDingDingPhoneNumber(String n) {
        if (!dingdingPhoneNumberFormat.matcher(n).matches()) {
            throw new ApiMessageInterceptionException(argerr("invalid phone number[%s], the DingDing phone number is like +86-12388889999", n));
        }
    }

    private void validate(APICreateSNSDingTalkEndpointMsg msg) {
        validateURL(msg.getUrl());

        if (msg.getAtPersonPhoneNumbers() != null) {
            msg.getAtPersonPhoneNumbers().forEach(this::errorOnWrongDingDingPhoneNumber);

            msg.setAtPersonPhoneNumbers(new ArrayList<>(new HashSet<>(msg.getAtPersonPhoneNumbers())));
        }

        if (msg.getAtPersonList() != null) {
            msg.getAtPersonList().keySet().forEach(this::errorOnWrongDingDingPhoneNumber);
        }
    }

    private void validate(APICreateSNSHttpEndpointMsg msg) {
        validateURL(msg.getUrl());
        if ((msg.getUsername() == null && msg.getPassword() != null) || (msg.getUsername() != null && msg.getPassword() == null)) {
            throw new ApiMessageInterceptionException(argerr("username and password must either absent at all or present with each other"));
        }
    }

    private void validate(APICreateSNSEmailEndpointMsg msg) {
        EmailValidator validator = EmailValidator.getInstance(true);

        if (msg.getEmail() == null && (msg.getEmails() == null || msg.getEmails().isEmpty())) {
            throw new ApiMessageInterceptionException(argerr("can not create sns email endpoint without any email address"));
        }

        if (msg.getEmail() != null && !validator.isValid(msg.getEmail())) {
            throw new ApiMessageInterceptionException(argerr("invalid email address[%s]", msg.getEmail()));
        }

        if (msg.getEmails() != null && !msg.getEmails().isEmpty()) {
            msg.setEmails(msg.getEmails().stream().distinct().collect(Collectors.toList()));

            List<String> errorEmails = msg.getEmails().stream().filter(email -> !validator.isValid(email)).collect(Collectors.toList());

            if (!errorEmails.isEmpty()) {
                throw new ApiMessageInterceptionException(argerr("invalid email address[%s]", errorEmails));
            }
        }
    }

    private void validate(APICreateSNSFeiShuEndpointMsg msg) {
        validateURL(msg.getUrl());
    }

    private void validate(APICreateSNSWeComEndpointMsg msg) {
        validateURL(msg.getUrl());
    }

    private void validate(APIUpdateSNSFeiShuEndpointMsg msg) {
        if (StringUtils.isNotBlank(msg.getUrl()))
            validateURL(msg.getUrl());
    }

    private void validate(APIUpdateSNSWeComEndpointMsg msg) {

        if (StringUtils.isNotBlank(msg.getUrl()))
            validateURL(msg.getUrl());
    }

    private void validate(APIUpdateAtPersonOfAtDingTalkEndpointMsg msg) {
        if (StringUtils.isNotBlank(msg.getPhoneNumber())) {
            errorOnWrongDingDingPhoneNumber(msg.getPhoneNumber());

            if (Q.New(SNSDingTalkAtPersonVO.class).eq(SNSDingTalkAtPersonVO_.endpointUuid, msg.getEndpointUuid())
                    .eq(SNSDingTalkAtPersonVO_.phoneNumber, msg.getPhoneNumber()).isExists()) {
                throw new ApiMessageInterceptionException(argerr("phone number[%s] already exists", msg.getPhoneNumber()));
            }
        }
    }

    private void validate(APIUpdateAtPersonOfAtFeiShuEndpointMsg msg) {
        if (StringUtils.isNotBlank(msg.getUserId())
                && Q.New(SNSFeiShuAtPersonVO.class).eq(SNSFeiShuAtPersonVO_.endpointUuid, msg.getEndpointUuid())
                .eq(SNSFeiShuAtPersonVO_.userId, msg.getUserId())
                .isExists())
            throw new ApiMessageInterceptionException(argerr("userId [%s] already exists", msg.getUserId()));
    }

    private void validate(APIUpdateAtPersonOfAtWeComEndpointMsg msg) {
        if (StringUtils.isNotBlank(msg.getUserId())
                && Q.New(SNSWeComAtPersonVO.class).eq(SNSWeComAtPersonVO_.endpointUuid, msg.getEndpointUuid())
                .eq(SNSWeComAtPersonVO_.userId, msg.getUserId())
                .isExists())
            throw new ApiMessageInterceptionException(argerr("userId [%s] already exists", msg.getUserId()));
    }
}
