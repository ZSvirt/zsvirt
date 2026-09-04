package org.zstack.loginControl.api

import org.zstack.loginControl.api.APIUpdateAccessControlRuleEvent

doc {
	title "UpdateAccessControlRule"

	category "loginControl"

	desc """更新IP访问控制规则请求"""

	rest {
		request {
			url "PUT /v1/login-control/access-control/rules/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateAccessControlRuleMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateAccessControlRule"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.5.1"
				}
				column {
					name "name"
					enclosedIn "updateAccessControlRule"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.5.1"
				}
				column {
					name "description"
					enclosedIn "updateAccessControlRule"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.5.1"
				}
				column {
					name "rule"
					enclosedIn "updateAccessControlRule"
					desc "IP访问控制规则"
					location "body"
					type "String"
					optional true
					since "3.5.1"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.5.1"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.5.1"
				}
			}
		}

		response {
			clz APIUpdateAccessControlRuleEvent.class
		}
	}
}