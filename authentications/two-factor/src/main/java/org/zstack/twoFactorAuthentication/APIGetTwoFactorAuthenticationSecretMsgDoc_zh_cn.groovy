package org.zstack.twoFactorAuthentication

import org.zstack.twoFactorAuthentication.APIGetTwoFactorAuthenticationSecretReply

doc {
	title "GetTwoFactorAuthenticationSecret"

	category "twoFactorAuthentication"

	desc """获取双因子认证密匙"""

	rest {
		request {
			url "GET /v1/twofactorauthentication/secret"



			clz APIGetTwoFactorAuthenticationSecretMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn ""
					desc "资源名称"
					location "query"
					type "String"
					optional false
					since "4.10.0"
				}
				column {
					name "password"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional false
					since "4.10.0"
				}
				column {
					name "captchaUuid"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "4.10.0"
				}
				column {
					name "verifyCode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "4.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "4.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "4.10.0"
				}
				column {
					name "type"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional false
					since "5.0.0"
					values ("account","ldap")
				}
			}
		}

		response {
			clz APIGetTwoFactorAuthenticationSecretReply.class
		}
	}
}