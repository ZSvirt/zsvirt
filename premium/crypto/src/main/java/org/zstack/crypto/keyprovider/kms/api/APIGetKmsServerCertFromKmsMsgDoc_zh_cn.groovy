package org.zstack.crypto.keyprovider.kms.api

import org.zstack.crypto.keyprovider.kms.api.APIGetKmsServerCertFromKmsEvent

doc {
	title "GetKmsServerCertFromKms"

	category "keyProvider"

	desc """从KMS获取服务端证书"""

	rest {
		request {
			url "PUT /v1/key-providers/kms/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetKmsServerCertFromKmsMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "getKmsServerCertFromKms"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "5.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.0.0"
				}
			}
		}

		response {
			clz APIGetKmsServerCertFromKmsEvent.class
		}
	}
}