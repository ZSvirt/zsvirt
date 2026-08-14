package org.zstack.crypto.keyprovider.nkp.api

import org.zstack.crypto.keyprovider.nkp.api.APIBackupNkpEvent

doc {
	title "BackupNkp"

	category "keyProvider"

	desc """备份原生密钥提供程序"""

	rest {
		request {
			url "PUT /v1/key-providers/nkp/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIBackupNkpMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "backupNkp"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "5.0.0"
				}
				column {
					name "password"
					enclosedIn "backupNkp"
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
			clz APIBackupNkpEvent.class
		}
	}
}