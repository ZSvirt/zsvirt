package org.zstack.ldap.api

import org.zstack.header.errorcode.ErrorCode
import org.zstack.ldap.entity.LdapServerInventory

doc {

    title "LDAP服务器清单列表"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "4.3.0"
	}
    ref {
        name "error"
        path "org.zstack.ldap.api.APIQueryLdapServerReply.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
        type "ErrorCode"
        since "4.3.0"
        clz ErrorCode.class
    }
    ref {
        name "inventories"
        path "org.zstack.ldap.api.APIQueryLdapServerReply.inventories"
        desc "LDAP 服务器清单列表"
        type "List"
        since "4.3.0"
        clz LdapServerInventory.class
    }
}
