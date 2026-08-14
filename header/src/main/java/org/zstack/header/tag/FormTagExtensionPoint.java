package org.zstack.header.tag;

import org.zstack.utils.function.Function;

import java.util.Map;

public interface FormTagExtensionPoint {
    Map<String, Function<String, String>> getTagMappers(Class clz);
}
