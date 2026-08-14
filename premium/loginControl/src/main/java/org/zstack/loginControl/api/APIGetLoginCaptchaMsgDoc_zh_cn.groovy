package org.zstack.loginControl.api

import org.zstack.loginControl.api.APIGetLoginCaptchaReply

doc {
	title "GetLoginCaptcha"

	category "loginControl"

	desc """获取登录验证码"""

	rest {
		request {
			url "GET /v1/login/control/captcha"



			clz APIGetLoginCaptchaMsg.class

			desc """"""

			params {

				column {
					name "resourceName"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "loginType"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "captchaUuid"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIGetLoginCaptchaReply.class
		}
	}
}