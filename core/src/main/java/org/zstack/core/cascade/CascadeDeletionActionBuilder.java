package org.zstack.core.cascade;

import java.util.Arrays;

/**
 */
public class CascadeDeletionActionBuilder {
    private String rootIssuer;
    private Object rootContext;
    private String parentIssuer;
    private Object parentContext;

    public CascadeDeletionActionBuilder setRootIssuer(String rootIssuer) {
        this.rootIssuer = rootIssuer;
        return this;
    }

    public CascadeDeletionActionBuilder setRootContext(Object rootContext) {
        this.rootContext = rootContext;
        return this;
    }

    public CascadeDeletionActionBuilder setParentIssuer(String parentIssuer) {
        this.parentIssuer = parentIssuer;
        return this;
    }

    public CascadeDeletionActionBuilder setParentContext(Object parentContext) {
        this.parentContext = parentContext;
        return this;
    }

    private CascadeAction build(String code) {
        assert rootIssuer != null;
        CascadeAction action = new CascadeAction();
        action.setRootIssuer(rootIssuer);
        action.setRootIssuerContext(Arrays.asList(rootContext));
        if (parentIssuer == null) {
            parentIssuer = rootIssuer;
        }
        action.setParentIssuer(parentIssuer);
        if (parentContext == null) {
            parentContext = rootContext;
        }
        action.setParentIssuerContext(Arrays.asList(parentContext));
        action.setActionCode(code);
        return action;
    }

    public CascadeAction buildCheckAction() {
        return build(CascadeConstant.DELETION_CHECK_CODE);
    }

    public CascadeAction buildDeleteAction() {
        return build(CascadeConstant.DELETION_DELETE_CODE);
    }

    public CascadeAction buildCleanupAction() {
        return build(CascadeConstant.DELETION_CLEANUP_CODE);
    }
}
