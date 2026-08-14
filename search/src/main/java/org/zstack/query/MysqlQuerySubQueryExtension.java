package org.zstack.query;

import org.zstack.header.query.APIQueryMessage;

import java.util.List;

/**
 */
public interface MysqlQuerySubQueryExtension {
    String makeSubquery(APIQueryMessage msg, Class inventoryClass);

    List<String> getEscapeConditionNames();
}
