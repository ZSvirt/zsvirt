package org.zstack.network.service;

import org.zstack.core.ScatteredValidator;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 10:43 2021/10/26
 */
public class CentralizedDnsValidator extends ScatteredValidator {
    private static List<Method> methods;

    static {
        // method signature: static void xxx(String hostUuid)
        methods = collectValidatorMethods(CentralizedDnsValidatorMethod.class, String.class);
    }

    public boolean validate(String vmUuid) {
        try {
            invokeValidatorMethods(methods, vmUuid);
        } catch (SkipApplyDecentralizedDnsException e) {
            return true;
        }

        return false;
    }
}
