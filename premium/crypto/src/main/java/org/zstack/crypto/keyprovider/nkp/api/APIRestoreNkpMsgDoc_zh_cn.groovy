package org.zstack.crypto.keyprovider.nkp.api

import org.zstack.crypto.keyprovider.nkp.api.APIRestoreNkpEvent

doc {
	title "RestoreNkp"

	category "keyProvider"

	desc """导入原生密钥提供程序"""

	rest {
		request {
			url "PUT /v1/key-providers/nkp/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRestoreNkpMsg.class

			desc """"""

			params {

				column {
					name "contentBase64"
					enclosedIn "restoreNkp"
					desc "备份内容（Base64 编码）"
					location "body"
					type "String"
					optional false
					since "5.0.0"
				}
				column {
					name "password"
					enclosedIn "restoreNkp"
					desc "备份保护密码"
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
			clz APIRestoreNkpEvent.class
		}
	}
}