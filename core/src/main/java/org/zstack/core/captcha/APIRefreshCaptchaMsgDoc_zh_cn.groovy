package org.zstack.core.captcha

import org.zstack.core.captcha.APIRefreshCaptchaReply

doc {
	title "RefreshCaptcha"

	category "captcha"

	desc """刷新验证码"""

	rest {
		request {
			url "GET /v1/captcha/refresh"



			clz APIRefreshCaptchaMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
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
			}
		}

		response {
			clz APIRefreshCaptchaReply.class
		}
	}
}