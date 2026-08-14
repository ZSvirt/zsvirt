package org.zstack.sso.service;

import org.zstack.header.message.NeedJsonSchema;
import org.zstack.sso.header.CasClientInventory;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: DaoDao
 * @Date: 2022/9/26
 */
public class SSOCanonicalEvents {
    public static final String CAS_MODIFY_CONFIGURE_PATH = "/sso/cas/configure";

    @NeedJsonSchema
    public static class CasModifyConfigureData implements Serializable {
        private CasClientInventory inventory;
        private List<String> reasons;

        public CasClientInventory getInventory() {
            return inventory;
        }

        public void setInventory(CasClientInventory inventory) {
            this.inventory = inventory;
        }

        public List<String> getReasons() {
            return reasons;
        }

        public void setReasons(List<String> reasons) {
            this.reasons = reasons;
        }
    }
}
