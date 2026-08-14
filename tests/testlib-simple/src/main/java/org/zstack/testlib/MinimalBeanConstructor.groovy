package org.zstack.testlib

import org.zstack.core.CoreGlobalProperty
import org.zstack.core.StartMode

class MinimalBeanConstructor extends WebBeanConstructor{

    private String MINIMAL_XML_NAME = "zstack-minimal.xml"

    @Override
    void generateSpringConfig() {
        CoreGlobalProperty.BEAN_CONF = MINIMAL_XML_NAME
        CoreGlobalProperty.START_MODE = StartMode.MINIMAL
    }
}
