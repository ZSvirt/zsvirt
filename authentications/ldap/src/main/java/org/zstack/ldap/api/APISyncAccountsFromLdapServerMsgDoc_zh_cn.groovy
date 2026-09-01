package org.zstack.ldap.api

import org.zstack.ldap.api.APISyncAccountsFromLdapServerEvent

doc {
	title "SyncAccountsFromLdapServer"

	category "ldap"

	desc """从LDAP服务器上同步用户"""

	rest {
		request {
			url "PUT /v1/ldap/servers/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISyncAccountsFromLdapServerMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "syncAccountsFromLdapServer"
					desc "LDAP服务器的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "4.3.0"
				}
				column {
					name "createAccountStrategy"
					enclosedIn "syncAccountsFromLdapServer"
					desc "从LDAP服务器同步时，对于LDAP服务器中新创建的用户，该虚拟化平台的处理策略，是创建对应的account还是无动作"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "deleteAccountStrategy"
					enclosedIn "syncAccountsFromLdapServer"
					desc "从LDAP服务器同步时，对于LDAP服务器中已删除的用户，该虚拟化平台的处理策略，是删除对应的account、标记已删除还是无动作"
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
			clz APISyncAccountsFromLdapServerEvent.class
		}
	}
}