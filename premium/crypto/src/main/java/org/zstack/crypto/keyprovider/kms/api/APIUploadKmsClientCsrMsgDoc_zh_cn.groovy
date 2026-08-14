package org.zstack.crypto.keyprovider.kms.api

import org.zstack.crypto.keyprovider.kms.api.APIUploadKmsClientCsrEvent

doc {
	title "UploadKmsClientCsr"

	category "keyProvider"

	desc """上传KMS客户端CSR与私钥"""

	rest {
		request {
			url "PUT /v1/key-providers/kms/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUploadKmsClientCsrMsg.class

			desc """"""

			params {

				column {
					name "csrPem"
					enclosedIn "uploadKmsClientCsr"
					desc "CSR"
					location "body"
					type "String"
					optional false
					since "5.0.0"
				}
				column {
					name "csrKeyPem"
					enclosedIn "uploadKmsClientCsr"
					desc "CSR私钥"
					location "body"
					type "String"
					optional false
					since "5.0.0"
				}
				column {
					name "uuid"
					enclosedIn "uploadKmsClientCsr"
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
			clz APIUploadKmsClientCsrEvent.class
		}
	}
}