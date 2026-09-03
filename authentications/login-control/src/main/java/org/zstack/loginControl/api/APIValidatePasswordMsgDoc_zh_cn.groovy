package org.zstack.loginControl.api

import org.zstack.loginControl.api.APIValidatePasswordReply

doc {
	title "ValidatePassword"

	category "loginControl"

	desc """校验用户身份请求"""

	rest {
		request {
			url "PUT /v1/password/verify"



			clz APIValidatePasswordMsg.class

			desc """在删除等敏感操作时候支持验证账号当前登录密码"""

			params {

				column {
					name "loginName"
					enclosedIn "validatePassword"
					desc "用户名"
					location "body"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "password"
					enclosedIn "validatePassword"
					desc "用户密码"
					location "body"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "loginType"
					enclosedIn "validatePassword"
					desc "登录类型"
					location "body"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.10.0"
				}
			}
		}

		response {
			clz APIValidatePasswordReply.class
		}
	}
}