package org.zstack.crypto.keyprovider.nkp.api

import org.zstack.crypto.keyprovider.nkp.api.APIParseNkpRestoreEvent

doc {
	title "ParseNkpRestore"

	category "keyProvider"

	desc """解析原生密钥提供程序导入内容"""

	rest {
		request {
			url "PUT /v1/key-providers/nkp/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIParseNkpRestoreMsg.class

			desc """"""

			params {

				column {
					name "contentBase64"
					enclosedIn "parseNkpRestore"
					desc "导入内容（Base64 编码）"
					location "body"
					type "String"
					optional false
					since "5.0.0"
				}
				column {
					name "password"
					enclosedIn "parseNkpRestore"
					desc "导入保护密码"
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
			clz APIParseNkpRestoreEvent.class
		}
	}
}