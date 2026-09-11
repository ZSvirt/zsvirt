package org.zstack.query;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryStruct;
import org.zstack.utils.DebugUtils;

import java.util.List;
import java.util.Map;

public class ExpandedQueryAliasInfo {
    Class<?> queryMessageClass;
    Class<?> inventoryClassDefiningThisAlias;
    public Class<?> inventoryClass;
    String expandField;
    public String alias;
    boolean isFromAnnotation;

    void check(Map<Class<?>, List<ExpandedQueryStruct>> expandedQueryStructs) {
        String[] slices = expandField.split("\\.");
        String firstExpand = slices[0];

        if (isFromAnnotation) {
            ExpandedQueries eqs = inventoryClassDefiningThisAlias.getAnnotation(ExpandedQueries.class);
            DebugUtils.Assert(eqs != null, String.format("inventory[%s] having annotation[ExpandedQueryAliases] must also have annotation[ExpandedQueries]",
                    inventoryClassDefiningThisAlias.getName()));

            for (ExpandedQuery at : eqs.value()) {
                if (at.expandedField().equals(firstExpand)) {
                    return;
                }
            }

            throw new CloudRuntimeException(String.format("inventory[%s] has an expanded query alias[%s]," +
                            " but it doesn't have an expand query that has expandedField[%s]",
                    inventoryClassDefiningThisAlias.getName(), alias, firstExpand));
        } else {
            List<ExpandedQueryStruct> expds = expandedQueryStructs.get(inventoryClassDefiningThisAlias);
            for (ExpandedQueryStruct s : expds) {
                if (s.getExpandedField().equals(firstExpand)) {
                    return;
                }
            }

            throw new CloudRuntimeException(String.format("inventory[%s] has an expanded query alias[%s](added by package description]," +
                    " but the extension doesn't declare any expanded query having expandedField[%s]",
                    inventoryClassDefiningThisAlias, alias, firstExpand));
        }
    }
}
