package org.zstack.core.validation;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.Component;
import org.zstack.header.core.validation.Validator;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.utils.TypeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.inerr;

/**
 */
public class ValidationFacadeImpl implements ValidationFacade, Component {
    private Map<Class, List<Validator>> validators = new HashMap<Class, List<Validator>>();

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ErrorFacade errf;

    @Override
    public void validateErrorByException(Object obj) {
        ErrorCode err = validateErrorByErrorCode(obj);
        if (err != null) {
            throw new OperationFailureException(err);
        }
    }

    @Override
    public ErrorCode validateErrorByErrorCode(Object obj) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof ConditionalValidation) {
            ConditionalValidation cond = (ConditionalValidation) obj;
            if (!cond.needValidation()) {
                return null;
            }
        }

        List<Class> classes = TypeUtils.getAllClassOfObject(obj);
        for (Class clz : classes) {
            List<Validator> vs = validators.get(clz);
            if (vs != null) {
                for (Validator v : vs) {
                    String err = v.validate(obj);
                    if (err != null) {
                        return inerr(err);
                    }
                }
            }
        }

        return null;
    }

    private void populateExtensions() {
        for (Validator ext : pluginRgty.getExtensionList(Validator.class)) {
            for (Class clazz : ext.supportedClasses()) {
                List<Validator> vs = validators.get(clazz);
                if (vs == null) {
                    vs = new ArrayList<Validator>();
                    validators.put(clazz, vs);
                }
                vs.add(ext);
            }
        }
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
