package org.zstack.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

class DynamicObjectMetadata {
    Map<String, Field> fields = new HashMap<>();
    Map<String, Method> methods = new HashMap<>();
}
