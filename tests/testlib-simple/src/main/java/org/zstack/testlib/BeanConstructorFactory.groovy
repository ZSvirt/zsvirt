package org.zstack.testlib

import org.zstack.core.StartMode

class BeanConstructorFactory {

    static BeanConstructor getBeanConstructor(StartMode mode) {
        if (mode == null) {
            return null
        }
        if (mode == StartMode.SIMULATOR) {
            return new BeanConstructor()
        }else if (mode == StartMode.MINIMAL) {
            return new MinimalBeanConstructor()
        }else {
            return new WebBeanConstructor()
        }
    }
}
