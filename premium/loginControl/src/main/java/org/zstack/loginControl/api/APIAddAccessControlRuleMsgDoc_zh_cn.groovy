package org.zstack.loginControl.api

import org.zstack.loginControl.api.APIAddAccessControlRuleEvent

doc {
	title "AddAccessControlRule"

	category "loginControl"

	desc """增加IP访问控制规则请求"""

	rest {
		request {
			url "POST /v1/login-control/access-control/rules"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddAccessControlRuleMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.5.1"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.5.1"
				}
				column {
					name "rule"
					enclosedIn "params"
					desc "IP访问控制规则"
					location "body"
					type "String"
					optional false
					since "3.5.1"
				}
				column {
					name "controlStrategy"
					enclosedIn "params"
					desc "IP访问控制策略"
					location "body"
					type "String"
					optional false
					since "3.5.1"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.5.1"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
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
			clz APIAddAccessControlRuleEvent.class
		}
	}
}