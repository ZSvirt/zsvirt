package org.zstack.crypto.keyprovider.nkp.api

import org.zstack.crypto.keyprovider.nkp.api.APICreateNkpEvent

doc {
	title "CreateNkp"

	category "keyProvider"

	desc """创建原生密钥提供程序"""

	rest {
		request {
			url "POST /v1/key-providers/nkp"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateNkpMsg.class

			desc """"""

			params {

				column {
					name "kdf"
					enclosedIn "params"
					desc "密钥派生函数"
					location "body"
					type "String"
					optional true
					since "5.0.0"
					values ("HKDF-SHA256")
				}
				column {
					name "saltPolicy"
					enclosedIn "params"
					desc "加盐策略"
					location "body"
					type "String"
					optional true
					since "5.0.0"
					values ("providerName")
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
			clz APICreateNkpEvent.class
		}
	}
}