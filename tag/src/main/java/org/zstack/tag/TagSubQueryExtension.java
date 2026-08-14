package org.zstack.tag;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.query.AbstractMysqlQuerySubQueryExtension;
import org.zstack.query.QueryUtils;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 */
public class TagSubQueryExtension extends AbstractMysqlQuerySubQueryExtension {
    public static String USER_TAG_NAME = "__userTag__";
    public static String SYS_TAG_NAME = "__systemTag__";

    private static List<String> IN_CONDITIONS;
    private static List<String> NOT_IN_CONDITIONS;

    static {
        IN_CONDITIONS = CollectionDSL.list(
                QueryOp.EQ.toString(),
                QueryOp.GT.toString(),
                QueryOp.GT_AND_EQ.toString(),
                QueryOp.LT.toString(),
                QueryOp.LT_AND_EQ.toString(),
                QueryOp.IN.toString(),
                QueryOp.LIKE.toString(),
                QueryOp.NOT_NULL.toString()
        );

        NOT_IN_CONDITIONS = CollectionDSL.list(
                QueryOp.NOT_EQ.toString(),
                QueryOp.NOT_IN.toString(),
                QueryOp.IS_NULL.toString(),
                QueryOp.NOT_LIKE.toString()
        );
    }

    private String reverseOpIfNeed(QueryCondition cond) {
        if (QueryOp.NOT_EQ.equals(cond.getOp())) {
            return QueryOp.EQ.toString();
        } else if (QueryOp.NOT_IN.equals(cond.getOp())) {
            return QueryOp.IN.toString();
        } else if (QueryOp.IS_NULL.equals(cond.getOp())) {
            return QueryOp.NOT_NULL.toString();
        } else if (QueryOp.NOT_LIKE.equals(cond.getOp())) {
            return QueryOp.LIKE.toString();
        } else {
            return cond.getOp();
        }
    }

    private String buildCondition(String field, QueryCondition cond) {
        if (QueryOp.IN.equals(cond.getOp()) || QueryOp.NOT_IN.equals(cond.getOp())) {
            String[] values = cond.getValue().split(",");
            List<String> vals = new ArrayList<String>();
            for (String val : values) {
                vals.add(String.format("'%s'", val));
            }

            return String.format("%s %s (%s)", field, reverseOpIfNeed(cond), StringUtils.join(vals, ","));
        } else if (QueryOp.IS_NULL.equals(cond.getOp()) || QueryOp.NOT_NULL.equals(cond.getOp())) {
            return String.format("%s %s", field, reverseOpIfNeed(cond));
        } else {
            return String.format("%s %s '%s'", field, reverseOpIfNeed(cond), cond.getValue());
        }
    }

    private String chooseOp (QueryCondition cond) {
        if (IN_CONDITIONS.contains(cond.getOp())) {
            return "in";
        }

        if (NOT_IN_CONDITIONS.contains(cond.getOp())) {
            return "not in";
        }

        throw new CloudRuntimeException(String.format("invalid comparison operator[%s]; %s", cond.getOp(), JSONObjectUtil.toJsonString(cond)));
    }

    private String getResourceTypeString(Class entityClass) {
        List<String> rtypes = new ArrayList<String>();
        while (entityClass != Object.class) {
            rtypes.add(String.format("'%s'", entityClass.getSimpleName()));
            entityClass = entityClass.getSuperclass();
        }

        return StringUtils.join(rtypes, ",");
    }

    @Override
    public String makeSubquery(APIQueryMessage msg, Class inventoryClass) {
        List<String> resultQuery = new ArrayList<String>();
        Class entityClass = QueryUtils.getEntityClassFromInventoryClass(inventoryClass);
        String primaryKey = QueryUtils.getPrimaryKeyNameFromEntityClass(entityClass);
        String typeString = getResourceTypeString(entityClass);
        String invname = inventoryClass.getSimpleName().toLowerCase();

        for (QueryCondition cond : msg.getConditions()) {
            if (cond.getName().equals(USER_TAG_NAME)) {
                List<String> condStrs = new ArrayList<String>();
                condStrs.add(buildCondition("user.tag", cond));
                condStrs.add(String.format("user.resourceType in (%s)", typeString));
                resultQuery.add(String.format("%s.%s %s (select user.resourceUuid from UserTagVO user where %s)",
                        invname, primaryKey, chooseOp(cond), StringUtils.join(condStrs, " and ")));
            } else if (cond.getName().equals(SYS_TAG_NAME)) {
                List<String> condStrs = new ArrayList<String>();
                condStrs.add(buildCondition("sys.tag", cond));
                condStrs.add(String.format("sys.resourceType in (%s)", typeString));
                resultQuery.add(String.format("%s.%s %s (select sys.resourceUuid from SystemTagVO sys where %s)",
                        invname, primaryKey, chooseOp(cond), StringUtils.join(condStrs, " and ")));
            }
        }

        if (resultQuery.isEmpty()) {
            return null;
        } else {
            return StringUtils.join(resultQuery, " and ");
        }
    }

    @Override
    public List<String> getEscapeConditionNames() {
        return Arrays.asList(USER_TAG_NAME, SYS_TAG_NAME);
    }
}
