package org.zstack.loginControl.api

import org.zstack.loginControl.api.APIDeleteAccessControlRuleEvent

doc {
	title "DeleteAccessControlRule"

	category "loginControl"

	desc """删除IP访问控制规则请求"""

	rest {
		request {
			url "DELETE /v1/login-control/access-control/rules/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteAccessControlRuleMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.5.1"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "3.5.1"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.5.1"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.5.1"
				}
			}
		}

		response {
			clz APIDeleteAccessControlRuleEvent.class
		}
	}
}