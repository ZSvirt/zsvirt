package org.zstack.core.convert;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Component;
import org.zstack.core.encrypt.EncryptFacade;
import org.zstack.core.encrypt.EncryptGlobalConfig;
import org.zstack.header.core.encrypt.PasswordEncryptType;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: DaoDao
 * @Date: 2023/3/9
 */
@Component
@Converter
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SpecialDataConverter implements AttributeConverter<String, String> {
    private static final CLogger logger = Utils.getLogger(SpecialDataConverter.class);

    private static EncryptFacade encryptFacade;


    @Autowired
    public void init(EncryptFacade encryptFacade){
        SpecialDataConverter.encryptFacade = encryptFacade;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (PasswordEncryptType.None.toString().equals(EncryptGlobalConfig.ENABLE_PASSWORD_ENCRYPT.value(String.class))) {
            return attribute;
        }
        if (StringUtils.isEmpty(attribute)) {
            return attribute;
        }

        if (!isMobileNO(attribute) && !checkEmail(attribute)) {
            return attribute;
        }

        return encryptFacade.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (PasswordEncryptType.None.toString().equals(EncryptGlobalConfig.ENABLE_PASSWORD_ENCRYPT.value(String.class))) {
            return dbData;
        }

        if (StringUtils.isEmpty(dbData)) {
            return dbData ;
        }

        return encryptFacade.decrypt(dbData);
    }

    public static boolean isMobileNO(String mobiles) {
        try {
            Pattern p = Pattern
                    .compile("[1][3456789][0-9]{9}$");
            Matcher m = p.matcher(mobiles);
            return m.matches();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean checkEmail(String email) {
        try {
            String EMAIL_PATTERN = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
            Pattern pattern = Pattern.compile(EMAIL_PATTERN);;
            Matcher matcher = pattern.matcher(email);
            return matcher.matches();
        } catch (Exception e) {
            return false;
        }
    }
}
