package org.zstack.crypto.keyprovider.api

import org.zstack.crypto.keyprovider.api.APIRekeyKeyProviderRefsEvent

doc {
	title "RekeyKeyProviderRefs"

	category "keyProvider"

	desc """对指定密钥重新加密"""

	rest {
		request {
			url "PUT /v1/key-providers/{providerUuid}/rekey"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRekeyKeyProviderRefsMsg.class

			desc """"""

			params {

				column {
					name "refIds"
					enclosedIn "rekeyKeyProviderRefs"
					desc "需要重加密的密钥引用ID列表"
					location "body"
					type "List"
					optional true
					since "5.0.0"
				}
				column {
					name "resourceUuids"
					enclosedIn "rekeyKeyProviderRefs"
					desc "关联资源UUID列表，与resourceType配对使用"
					location "body"
					type "List"
					optional true
					since "5.0.0"
				}
				column {
					name "resourceType"
					enclosedIn "rekeyKeyProviderRefs"
					desc "关联资源类型，与resourceUuids配对使用"
					location "body"
					type "String"
					optional true
					since "5.0.0"
				}
				column {
					name "providerUuid"
					enclosedIn "rekeyKeyProviderRefs"
					desc "密钥提供程序UUID"
					location "url"
					type "String"
					optional false
					since "5.0.0"
				}
				column {
					name "rekeyAll"
					enclosedIn "rekeyKeyProviderRefs"
					desc "是否执行全量重加密；为true时不能再指定refIds或resourceType/resourceUuids"
					location "body"
					type "boolean"
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
			clz APIRekeyKeyProviderRefsEvent.class
		}
	}
}