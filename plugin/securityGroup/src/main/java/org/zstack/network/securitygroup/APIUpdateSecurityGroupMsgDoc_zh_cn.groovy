package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.APIUpdateSecurityGroupEvent

doc {
	title "UpdateSecurityGroup"

	category "securityGroup"

	desc """更新安全组"""

	rest {
		request {
			url "PUT /v1/security-groups/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateSecurityGroupMsg.class

			desc """更新安全组"""

			params {

				column {
					name "uuid"
					enclosedIn "updateSecurityGroup"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "updateSecurityGroup"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "updateSecurityGroup"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIUpdateSecurityGroupEvent.class
		}
	}
}