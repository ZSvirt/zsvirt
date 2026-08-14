package org.zstack.header.core.validation;

import java.util.List;

/**
 */
public interface Validator {
    List<Class> supportedClasses();

    String validate(Object obj);
}
