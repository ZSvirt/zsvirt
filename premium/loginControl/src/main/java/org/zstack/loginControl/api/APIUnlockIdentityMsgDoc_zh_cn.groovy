package org.zstack.loginControl.api

import org.zstack.loginControl.api.APIUnlockIdentityReply

doc {
	title "UnlockIdentity"

	category "loginControl"

	desc """解锁登录次数限制请求"""

	rest {
		request {
			url "GET /v1/login/control/unlock"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUnlockIdentityMsg.class

			desc """"""

			params {

				column {
					name "resourceName"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional false
					since "3.5.1"
				}
				column {
					name "loginType"
					enclosedIn ""
					desc "登录类型"
					location "query"
					type "String"
					optional false
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
			clz APIUnlockIdentityReply.class
		}
	}
}