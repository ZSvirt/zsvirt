package org.zstack.header.storage.volume.backup

import org.zstack.header.storage.volume.backup.APIQueryVolumeBackupReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryVolumeBackup"

	category "backup.volume"

	desc """查询卷备份记录"""

	rest {
		request {
			url "GET /v1/volume-backups"
			url "GET /v1/volume-backups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVolumeBackupMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVolumeBackupReply.class
		}
	}
}