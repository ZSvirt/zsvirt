package org.zstack.twoFactorAuthentication

import org.zstack.twoFactorAuthentication.APIResetTwoFactorAuthenticationSecretEvent

doc {
	title "ResetTwoFactorAuthenticationSecret"

	category "twoFactorAuthentication"

	desc """重置双因子认证密匙"""

	rest {
		request {
			url "PUT /v1/twofactorauthentication/secrets"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIResetTwoFactorAuthenticationSecretMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "resetTwoFactorAuthenticationSecret"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "4.10.0"
				}
				column {
					name "password"
					enclosedIn "resetTwoFactorAuthenticationSecret"
					desc ""
					location "body"
					type "String"
					optional false
					since "4.10.0"
				}
				column {
					name "captchaUuid"
					enclosedIn "resetTwoFactorAuthenticationSecret"
					desc ""
					location "body"
					type "String"
					optional true
					since "4.10.0"
				}
				column {
					name "verifyCode"
					enclosedIn "resetTwoFactorAuthenticationSecret"
					desc ""
					location "body"
					type "String"
					optional true
					since "4.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "4.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "4.10.0"
				}
				column {
					name "type"
					enclosedIn "resetTwoFactorAuthenticationSecret"
					desc ""
					location "body"
					type "String"
					optional false
					since "5.0.0"
					values ("account","ldap")
				}
			}
		}

		response {
			clz APIResetTwoFactorAuthenticationSecretEvent.class
		}
	}
}