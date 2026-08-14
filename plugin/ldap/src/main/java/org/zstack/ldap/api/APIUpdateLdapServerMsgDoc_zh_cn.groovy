package org.zstack.ldap.api

import org.zstack.ldap.api.APIUpdateLdapServerEvent

doc {
	title "UpdateLdapServer"

	category "ldap"

	desc """更新LDAP服务器"""

	rest {
		request {
			url "PUT /v1/ldap/servers/{ldapServerUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateLdapServerMsg.class

			desc """更新LDAP服务器"""

			params {

				column {
					name "ldapServerUuid"
					enclosedIn "updateLdapServer"
					desc "LDAP服务器的UUID"
					location "url"
					type "String"
					optional false
					since "4.3.0"
				}
				column {
					name "name"
					enclosedIn "updateLdapServer"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "description"
					enclosedIn "updateLdapServer"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "url"
					enclosedIn "updateLdapServer"
					desc "LDAP服务器的访问地址"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "base"
					enclosedIn "updateLdapServer"
					desc "LDAP服务器的查询BaseDN"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "username"
					enclosedIn "updateLdapServer"
					desc "访问LDAP服务器的用户名"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "password"
					enclosedIn "updateLdapServer"
					desc "密码"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "encryption"
					enclosedIn "updateLdapServer"
					desc "加密方式"
					location "body"
					type "String"
					optional true
					since "4.3.0"
					values ("None","TLS")
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
				column {
					name "serverType"
					enclosedIn "updateLdapServer"
					desc "LDAP服务器类型"
					location "body"
					type "String"
					optional true
					since "4.3.0"
					values ("OpenLdap","WindowsAD","Unknown")
				}
				column {
					name "usernameProperty"
					enclosedIn "updateLdapServer"
					desc "用户登录该虚拟化平台时使用哪个字段用作用户名"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "filter"
					enclosedIn "updateLdapServer"
					desc "从LDAP服务器同步时，使用的过滤器，确定哪些用户是需要同步的"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "syncCreatedAccountStrategy"
					enclosedIn "updateLdapServer"
					desc "在从LDAP服务器同步时，对于LDAP服务器中新创建的用户，该虚拟化平台的处理策略，是创建对应的account还是无动作"
					location "body"
					type "String"
					optional true
					since "4.3.0"
					values ("NoAction","CreateDisabledAccount","CreateAccount")
				}
				column {
					name "syncDeletedAccountStrategy"
					enclosedIn "updateLdapServer"
					desc "在从LDAP服务器同步时，对于LDAP服务器中已删除的用户，该虚拟化平台的处理策略，是删除对应的account、标记已删除还是无动作"
					location "body"
					type "String"
					optional true
					since "4.3.0"
					values ("NoAction","StaleAccount","DeleteAccount")
				}
			}
		}

		response {
			clz APIUpdateLdapServerEvent.class
		}
	}
}