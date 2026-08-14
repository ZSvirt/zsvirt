package org.zstack.crypto.keyprovider.kms.api

import org.zstack.crypto.keyprovider.kms.api.APICreateKmsEvent

doc {
	title "CreateKms"

	category "keyProvider"

	desc """创建KMS密钥提供程序"""

	rest {
		request {
			url "POST /v1/key-providers/kms"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateKmsMsg.class

			desc """"""

			params {

				column {
					name "endpoint"
					enclosedIn "params"
					desc "KMS地址"
					location "body"
					type "String"
					optional false
					since "5.0.0"
				}
				column {
					name "port"
					enclosedIn "params"
					desc "端口"
					location "body"
					type "Integer"
					optional false
					since "5.0.0"
				}
				column {
					name "kmipVersion"
					enclosedIn "params"
					desc "KMIP协议版本"
					location "body"
					type "String"
					optional true
					since "5.0.0"
					values ("1.0","1.1","1.2","1.3","1.4","2.0","2.1")
				}
				column {
					name "username"
					enclosedIn "params"
					desc "用户名"
					location "body"
					type "String"
					optional true
					since "5.0.0"
				}
				column {
					name "password"
					enclosedIn "params"
					desc "密码"
					location "body"
					type "String"
					optional true
					since "5.0.0"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "5.0.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "5.0.0"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "密钥提供程序类型"
					location "body"
					type "String"
					optional true
					since "5.0.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "5.0.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
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
			clz APICreateKmsEvent.class
		}
	}
}