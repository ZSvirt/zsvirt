package org.zstack.header.core.encrypt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EncryptionParamAllowed {
    /**
     * Encrypted fields are not allowed.
     * 
     * By default, except for the userTags/systemTags fields,
     * all fields owned by {@link org.zstack.header.message.APIMessage} classes are not allowed
     */
    String[] forbiddenFields() default {};

    String ACTION_CHECK_USER_INFO = "checkUserInfo";
    String ACTION_PUT_USER_INFO_INTO_SYSTEM_TAG = "putUserInfoIntoSystemTag";

    /**
     * Check the result of parsing from the digital envelope
     *
     * - (Default) {@link #ACTION_CHECK_USER_INFO}
     *   checks whether the user tag in the digital envelope matches the user of the APIMessage session
     *   Mismatch will throw an error
     * - {@link #ACTION_PUT_USER_INFO_INTO_SYSTEM_TAG}
     *   system places the user data in the envelope in the system tag,
     *   and this process will not generate errors and interrupts
     *
     * It is recommended to enable this option for all APIs
     * that support digital envelopes except login
     */
    String[] actions() default { ACTION_CHECK_USER_INFO };
}
