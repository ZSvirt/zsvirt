package org.zstack.sdk;

import java.util.ArrayList;

/**
 * Created by xing5 on 2016/12/13.
 */
public abstract class QueryAction extends AbstractAction {
    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.util.List<String> conditions = new ArrayList<>();

    @Param(required = false)
    public java.lang.Integer limit;

    @Param(required = false)
    public java.lang.Integer start;

    @Param(required = false)
    public Boolean count;

    @Param(required = false)
    public java.lang.String groupBy;

    @Param(required = false)
    public Boolean replyWithCount;

    @Param(required = false)
    public java.lang.String sortBy;

    @Param(required = false, validValues = {"asc","desc"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String sortDirection;

    @Param(required = false)
    public java.util.List fields;

    @Param(required = false)
    public String sessionId;

    @Param(required = false)
    public String accessKeyId;

    @Param(required = false)
    public String accessKeySecret;

    @Param(required = false)
    public String requestIp;

    @Param(required = false)
    public String filterName;
}
