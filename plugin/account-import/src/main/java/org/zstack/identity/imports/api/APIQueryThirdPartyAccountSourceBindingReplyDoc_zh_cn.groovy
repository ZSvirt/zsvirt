package org.zstack.identity.imports.api

import org.zstack.header.errorcode.ErrorCode
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefInventory

doc {

    title "查询第三方用户来源绑定关系清单列表"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "4.3.0"
	}
    ref {
        name "error"
        path "org.zstack.ldap.api.APIQueryLdapBindingReply.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
        type "ErrorCode"
        since "4.3.0"
        clz ErrorCode.class
    }
    ref {
        name "inventories"
        path "org.zstack.ldap.api.APIQueryLdapBindingReply.inventories"
        desc "第三方用户来源绑定关系清单列表"
        type "List"
        since "4.3.0"
        clz AccountThirdPartyAccountSourceRefInventory.class
    }
}
