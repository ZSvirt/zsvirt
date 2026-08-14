package org.zstack.core.validator;

import org.zstack.core.CoreGlobalProperty;

@ValidateTarget(target = Vip.class)
public class VipValidator implements PropertyValidator {

    @Override
    public boolean validate(String name, String value, Object rule) throws PropertyValidatorException {
        Vip vip = (Vip) rule;
        if (value != null && !vip.value()) {
            String MN_VIP = CoreGlobalProperty.MN_VIP;
            if (CoreGlobalProperty.MN_VIP != null && value.equals(MN_VIP)) {
                throw new PropertyValidatorException("invalid ip: " + value + " for " + name);
            }
        }
        return true;
    }
}
