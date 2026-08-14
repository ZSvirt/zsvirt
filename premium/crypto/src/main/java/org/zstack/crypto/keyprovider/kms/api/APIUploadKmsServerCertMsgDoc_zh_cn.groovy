package org.zstack.crypto.keyprovider.kms.api

import org.zstack.crypto.keyprovider.kms.api.APIUploadKmsServerCertEvent

doc {
	title "UploadKmsServerCert"

	category "keyProvider"

	desc """上传KMS服务端证书"""

	rest {
		request {
			url "PUT /v1/key-providers/kms/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUploadKmsServerCertMsg.class

			desc """"""

			params {

				column {
					name "serverCertPem"
					enclosedIn "uploadKmsServerCert"
					desc "服务端证书"
					location "body"
					type "String"
					optional false
					since "5.0.0"
				}
				column {
					name "uuid"
					enclosedIn "uploadKmsServerCert"
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
			clz APIUploadKmsServerCertEvent.class
		}
	}
}