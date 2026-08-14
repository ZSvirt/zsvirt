package org.zstack.cloudformation;

import org.zstack.core.cascade.CascadeAction;
import org.zstack.header.identity.SessionInventory;

/**
 * Created by mingjian.deng on 2018/6/13.
 */
public class CFCascadeAction extends CascadeAction {
    private SessionInventory session;

    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }
}