package org.zstack.header.core.progress;

import java.util.List;

/**
 * Created by MaJin on 2019/7/3.
 */
public class TaskInfo {
    protected String name;
    protected String className;
    protected int index;
    protected long pendingTime;
    protected Long executionTime;
    protected String context;
    protected String apiId;
    protected String apiName;
    protected List<String> contextList;

    public List<String> getContextList() {
        return contextList;
    }

    public void setContextList(List<String> contextList) {
        this.contextList = contextList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public long getPendingTime() {
        return pendingTime;
    }

    public void setPendingTime(long pendingTime) {
        this.pendingTime = pendingTime;
    }

    public Long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Long executionTime) {
        this.executionTime = executionTime;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }
}
