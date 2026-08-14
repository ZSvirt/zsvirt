package org.zstack.core.cascade;

import org.zstack.header.exception.CloudRuntimeException;

/**
 */
public class CascadeAction implements Cloneable {
    private String parentIssuer;
    private String rootIssuer;
    private Object parentIssuerContext;
    private Object rootIssuerContext;
    private String actionCode;
    private boolean fullTraverse;

    public boolean isFullTraverse() {
        return fullTraverse;
    }

    public CascadeAction setFullTraverse(boolean fullTraverse) {
        this.fullTraverse = fullTraverse;
        return this;
    }

    public String getParentIssuer() {
        return parentIssuer;
    }

    public String getRootIssuer() {
        return rootIssuer;
    }

    public <T> T getParentIssuerContext() {
        return (T) parentIssuerContext;
    }

    public <T> T getRootIssuerContext() {
        return (T) rootIssuerContext;
    }

    public CascadeAction setParentIssuer(String parentIssuer) {
        this.parentIssuer = parentIssuer;
        return this;
    }

    public CascadeAction setRootIssuer(String rootIssuer) {
        this.rootIssuer = rootIssuer;
        return this;
    }

    public CascadeAction setParentIssuerContext(Object parentIssuerContext) {
        this.parentIssuerContext = parentIssuerContext;
        return this;
    }

    public CascadeAction setRootIssuerContext(Object rootIssuerContext) {
        this.rootIssuerContext = rootIssuerContext;
        return this;
    }

    public CascadeAction copy() {
        try {
            return (CascadeAction) this.clone();
        } catch (CloneNotSupportedException e) {
            throw new CloudRuntimeException(e);
        }
    }

    public String getActionCode() {
        return actionCode;
    }

    public CascadeAction setActionCode(String actionCode) {
        this.actionCode = actionCode;
        return this;
    }

    public boolean isActionCode(String...codes) {
        for (String code : codes) {
            if (actionCode.equals(code)) {
                return true;
            }
        }

        return false;
    }
}
