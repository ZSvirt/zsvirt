package org.zstack.header.identity.login

import org.zstack.header.identity.login.APIGetLoginProceduresReply

doc {
	title "GetLoginProcedures"

	category "login"

	desc """获取登录的认证步骤"""

	rest {
		request {
			url "GET /v1/login/procedures"



			clz APIGetLoginProceduresMsg.class

			desc """"""

			params {

				column {
					name "username"
					enclosedIn ""
					desc "用户名"
					location "query"
					type "String"
					optional false
					since "zsv 3.16.0"
				}
				column {
					name "loginType"
					enclosedIn ""
					desc "认证用户类型"
					location "query"
					type "String"
					optional false
					since "zsv 3.16.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.16.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.16.0"
				}
			}
		}

		response {
			clz APIGetLoginProceduresReply.class
		}
	}
}