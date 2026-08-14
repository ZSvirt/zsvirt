package org.zstack.header.host;

import org.zstack.utils.function.ValidateFunction;

import java.util.List;

public interface HypervisorMessageFactory {
    HypervisorType getHypervisorType();
    List<AddHostMsg> buildMessageFromFile(String content, ValidateFunction<AddHostMsg> validator);
}
