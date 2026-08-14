package org.zstack.header.core.encrypt

import org.zstack.header.core.encrypt.APIGetEncryptedFieldReply

doc {
	title "GetEncryptedField"

	category "crypto"

	desc """获取加密字段"""

	rest {
		request {
			url "GET /v1/encrypted/fields"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetEncryptedFieldMsg.class

			desc """"""

			params {

				column {
					name "encryptedType"
					enclosedIn ""
					desc "加密字段类型"
					location "query"
					type "String"
					optional true
					since "zsv 3.17.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.17.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.17.0"
				}
			}
		}

		response {
			clz APIGetEncryptedFieldReply.class
		}
	}
}