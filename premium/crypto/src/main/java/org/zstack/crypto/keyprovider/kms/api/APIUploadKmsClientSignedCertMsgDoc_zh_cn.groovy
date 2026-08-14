package org.zstack.crypto.keyprovider.kms.api

import org.zstack.crypto.keyprovider.kms.api.APIUploadKmsClientSignedCertEvent

doc {
	title "UploadKmsClientSignedCert"

	category "keyProvider"

	desc """上传KMS客户端回签证书"""

	rest {
		request {
			url "PUT /v1/key-providers/kms/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUploadKmsClientSignedCertMsg.class

			desc """"""

			params {

				column {
					name "signedClientCertPem"
					enclosedIn "uploadKmsClientSignedCert"
					desc "回签客户端证书"
					location "body"
					type "String"
					optional false
					since "5.0.0"
				}
				column {
					name "uuid"
					enclosedIn "uploadKmsClientSignedCert"
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
			clz APIUploadKmsClientSignedCertEvent.class
		}
	}
}