package org.zstack.crypto.keyprovider.kms.api

import org.zstack.crypto.keyprovider.kms.api.APIUploadKmsClientIdentityEvent

doc {
	title "UploadKmsClientIdentity"

	category "keyProvider"

	desc """上传KMS客户端证书与私钥"""

	rest {
		request {
			url "PUT /v1/key-providers/kms/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUploadKmsClientIdentityMsg.class

			desc """"""

			params {

				column {
					name "identityType"
					enclosedIn "uploadKmsClientIdentity"
					desc "身份来源类型"
					location "body"
					type "String"
					optional false
					since "5.0.0"
					values ("UPLOADED","PLATFORM")
				}
				column {
					name "kmsClientCertPem"
					enclosedIn "uploadKmsClientIdentity"
					desc "客户端证书"
					location "body"
					type "String"
					optional false
					since "5.0.0"
				}
				column {
					name "kmsClientKeyPem"
					enclosedIn "uploadKmsClientIdentity"
					desc "客户端私钥"
					location "body"
					type "String"
					optional false
					since "5.0.0"
				}
				column {
					name "uuid"
					enclosedIn "uploadKmsClientIdentity"
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
			clz APIUploadKmsClientIdentityEvent.class
		}
	}
}