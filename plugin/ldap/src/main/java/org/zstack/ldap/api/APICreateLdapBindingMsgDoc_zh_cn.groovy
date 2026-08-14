package org.zstack.ldap.api

import org.zstack.ldap.api.APICreateLdapBindingEvent

doc {
	title "CreateLdapBinding"

	category "ldap"

	desc """创建LDAP绑定"""

	rest {
		request {
			url "POST /v1/ldap/bindings"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateLdapBindingMsg.class

			desc """创建LDAP绑定"""

			params {

				column {
					name "ldapUid"
					enclosedIn "params"
					desc "LDAP UID"
					location "body"
					type "String"
					optional false
					since "4.3.0"
				}
				column {
					name "accountUuid"
					enclosedIn "params"
					desc "账户UUID"
					location "body"
					type "String"
					optional false
					since "4.3.0"
				}
				column {
					name "ldapServerUuid"
					enclosedIn "params"
					desc "LDAP服务器UUID。如果不指定，系统会选择当前正在使用的LDAP服务器的UUID"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.3.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.3.0"
				}
			}
		}

		response {
			clz APICreateLdapBindingEvent.class
		}
	}
}