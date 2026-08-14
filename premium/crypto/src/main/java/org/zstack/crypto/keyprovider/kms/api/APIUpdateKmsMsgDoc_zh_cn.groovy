package org.zstack.crypto.keyprovider.kms.api

import org.zstack.crypto.keyprovider.kms.api.APIUpdateKmsEvent

doc {
	title "UpdateKms"

	category "keyProvider"

	desc """更新KMS密钥提供程序"""

	rest {
		request {
			url "PUT /v1/key-providers/kms/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateKmsMsg.class

			desc """"""

			params {

				column {
					name "endpoint"
					enclosedIn "updateKms"
					desc "地址"
					location "body"
					type "String"
					optional true
					since "5.0.0"
				}
				column {
					name "port"
					enclosedIn "updateKms"
					desc "端口"
					location "body"
					type "Integer"
					optional true
					since "5.0.0"
				}
				column {
					name "kmipVersion"
					enclosedIn "updateKms"
					desc "KMIP协议版本"
					location "body"
					type "String"
					optional true
					since "5.0.0"
					values ("1.0","1.1","1.2","1.3","1.4","2.0","2.1")
				}
				column {
					name "username"
					enclosedIn "updateKms"
					desc "用户名"
					location "body"
					type "String"
					optional true
					since "5.0.0"
				}
				column {
					name "password"
					enclosedIn "updateKms"
					desc "密码"
					location "body"
					type "String"
					optional true
					since "5.0.0"
				}
				column {
					name "uuid"
					enclosedIn "updateKms"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "5.0.0"
				}
				column {
					name "description"
					enclosedIn "updateKms"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
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
			clz APIUpdateKmsEvent.class
		}
	}
}