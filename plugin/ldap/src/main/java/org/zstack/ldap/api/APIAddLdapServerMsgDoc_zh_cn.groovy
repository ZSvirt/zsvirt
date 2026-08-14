package org.zstack.ldap.api

import org.zstack.ldap.api.APIAddLdapServerEvent

doc {
	title "AddLdapServer"

	category "ldap"

	desc """添加LDAP服务器"""

	rest {
		request {
			url "POST /v1/ldap/servers"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddLdapServerMsg.class

			desc """添加LDAP服务器"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "4.3.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "url"
					enclosedIn "params"
					desc "LDAP服务器访问地址"
					location "body"
					type "String"
					optional false
					since "4.3.0"
				}
				column {
					name "base"
					enclosedIn "params"
					desc "LDAP服务查询BaseDN"
					location "body"
					type "String"
					optional false
					since "4.3.0"
				}
				column {
					name "username"
					enclosedIn "params"
					desc "访问LDAP服务器使用的用户名"
					location "body"
					type "String"
					optional false
					since "4.3.0"
				}
				column {
					name "password"
					enclosedIn "params"
					desc "密码"
					location "body"
					type "String"
					optional false
					since "4.3.0"
				}
				column {
					name "encryption"
					enclosedIn "params"
					desc "加密方式"
					location "body"
					type "String"
					optional false
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
					enclosedIn "params"
					desc "LDAP服务器类型"
					location "body"
					type "String"
					optional true
					since "4.3.0"
					values ("OpenLdap","WindowsAD","Unknown")
				}
				column {
					name "usernameProperty"
					enclosedIn "params"
					desc "用户登录该虚拟化平台时使用哪个字段用作用户名"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "filter"
					enclosedIn "params"
					desc "从LDAP服务器同步时，使用的过滤器，确定哪些用户是需要同步的"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "syncCreatedAccountStrategy"
					enclosedIn "params"
					desc "从LDAP服务器同步时，对于LDAP服务器中新创建的用户，该虚拟化平台的处理策略，是创建对应的account还是无动作"
					location "body"
					type "String"
					optional true
					since "4.3.0"
					values ("NoAction","CreateDisabledAccount","CreateAccount")
				}
				column {
					name "syncDeletedAccountStrategy"
					enclosedIn "params"
					desc "从LDAP服务器同步时，对于LDAP服务器中已删除的用户，该虚拟化平台的处理策略，是删除对应的account、标记已删除还是无动作"
					location "body"
					type "String"
					optional true
					since "4.3.0"
					values ("NoAction","StaleAccount","DeleteAccount")
				}
			}
		}

		response {
			clz APIAddLdapServerEvent.class
		}
	}
}