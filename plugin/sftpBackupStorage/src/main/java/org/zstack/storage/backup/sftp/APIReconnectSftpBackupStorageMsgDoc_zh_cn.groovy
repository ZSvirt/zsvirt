package org.zstack.storage.backup.sftp

import org.zstack.storage.backup.sftp.APIReconnectSftpBackupStorageEvent

doc {
	title "ReconnectSftpBackupStorage"

	category "storage.backup.sftp"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/backup-storage/sftp/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIReconnectSftpBackupStorageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "reconnectSftpBackupStorage"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIReconnectSftpBackupStorageEvent.class
		}
	}
}