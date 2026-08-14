package org.zstack.ldap.api

import org.zstack.header.errorcode.ErrorCode
import org.zstack.ldap.entity.LdapServerInventory

doc {

    title "LDAP服务器清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "4.3.0"
	}
    ref {
        name "error"
        path "org.zstack.ldap.api.APIAddLdapServerEvent.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
        type "ErrorCode"
        since "4.3.0"
        clz ErrorCode.class
    }
    ref {
        name "inventory"
        path "org.zstack.ldap.api.APIAddLdapServerEvent.inventory"
        desc "LDAP服务器清单"
        type "LdapServerInventory"
        since "4.3.0"
        clz LdapServerInventory.class
    }
}
