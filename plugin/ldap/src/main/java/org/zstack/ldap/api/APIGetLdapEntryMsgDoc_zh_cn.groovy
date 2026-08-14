package org.zstack.ldap.api

import org.zstack.ldap.api.APIGetLdapEntryReply

doc {
	title "GetLdapEntry"

	category "ldap"

	desc """查询LDAP/AD条目"""

	rest {
		request {
			url "GET /v1/ldap/entry"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetLdapEntryMsg.class

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
				column {
					name "limit"
					enclosedIn ""
					desc "最多返回的记录数，类似MySQL的limit"
					location "query"
					type "Integer"
					optional true
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
					name "searchAllAttributes"
					enclosedIn ""
					desc "是否列出所有查询到的属性，默认 false"
					location "query"
					type "boolean"
					optional true
					since "4.10.28"
				}
				column {
					name "returningAttributes"
					enclosedIn ""
					desc "指定补充返回的属性列表。当 searchAllAttributes 为 true 时，本属性无效"
					location "query"
					type "List"
					optional true
					since "4.10.28"
				}
			}
		}

		response {
			clz APIGetLdapEntryReply.class
		}
	}
}