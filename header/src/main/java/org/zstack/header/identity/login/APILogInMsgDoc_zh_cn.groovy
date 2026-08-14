package org.zstack.header.identity.login

import org.zstack.header.identity.APILogInReply

doc {
	title "LogIn"

	category "login"

	desc """登录"""

	rest {
		request {
			url "PUT /v1/login"



			clz APILogInMsg.class

			desc """"""

			params {

				column {
					name "username"
					enclosedIn "logIn"
					desc "用户名"
					location "body"
					type "String"
					optional false
					since "zsv 3.16.0"
				}
				column {
					name "password"
					enclosedIn "logIn"
					desc "用户密码"
					location "body"
					type "String"
					optional false
					since "zsv 3.16.0"
				}
				column {
					name "loginType"
					enclosedIn "logIn"
					desc "用户类型"
					location "body"
					type "String"
					optional false
					since "zsv 3.16.0"
				}
				column {
					name "captchaUuid"
					enclosedIn "logIn"
					desc "验证码uuid"
					location "body"
					type "String"
					optional true
					since "zsv 3.16.0"
				}
				column {
					name "verifyCode"
					enclosedIn "logIn"
					desc "验证码"
					location "body"
					type "String"
					optional true
					since "zsv 3.16.0"
				}
				column {
					name "clientInfo"
					enclosedIn "logIn"
					desc "客户端信息"
					location "body"
					type "Map"
					optional true
					since "zsv 3.16.0"
				}
				column {
					name "properties"
					enclosedIn "logIn"
					desc "登录属性"
					location "body"
					type "Map"
					optional true
					since "zsv 3.16.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.16.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.16.0"
				}
			}
		}

		response {
			clz APILogInReply.class
		}
	}
}