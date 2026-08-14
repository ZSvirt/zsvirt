package org.zstack.ldap.api

import org.zstack.ldap.api.APIGetLdapEntryReply

doc {
	title "GetCandidateLdapEntryForBinding"

	category "ldap"

	desc """查询可绑定的LDAP/AD条目(排除已绑定的LDAP/AD用户)"""

	rest {
		request {
			url "GET /v1/ldap/entries/candidates"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetCandidateLdapEntryForBindingMsg.class

			desc """"""

			params {

				column {
					name "ldapFilter"
					enclosedIn ""
					desc "查询条件"
					location "query"
					type "String"
					optional false
					since "4.3.0"
				}
				column {
					name "ldapServerUuid"
					enclosedIn ""
					desc "查询使用的LDAP服务器UUID。如果未指定会查找当前启用的LDAP服务器UUID"
					location "query"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "4.3.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "4.3.0"
				}
				column {
					name "limit"
					enclosedIn ""
					desc "最多返回的记录数，类似MySQL的limit"
					location "query"
					type "Integer"
					optional true
					since "4.3.0"
				}
			}
		}

		response {
			clz APIGetLdapEntryReply.class
		}
	}
}