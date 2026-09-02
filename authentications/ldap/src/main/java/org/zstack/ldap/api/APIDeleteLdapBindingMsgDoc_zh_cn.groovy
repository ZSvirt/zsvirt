package org.zstack.ldap.api

import org.zstack.ldap.api.APIDeleteLdapBindingEvent

doc {
	title "DeleteLdapBinding"

	category "ldap"

	desc """删除LDAP绑定"""

	rest {
		request {
			url "DELETE /v1/ldap/bindings/{accountUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteLdapBindingMsg.class

			desc """删除LDAP绑定"""

			params {

				column {
					name "accountUuid"
					enclosedIn ""
					desc "账户UUID"
					location "url"
					type "String"
					optional false
					since "4.3.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "4.3.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "4.3.0"
				}
			}
		}

		response {
			clz APIDeleteLdapBindingEvent.class
		}
	}
}