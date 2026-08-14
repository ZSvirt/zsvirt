package org.zstack.externalbackup

import org.zstack.externalbackup.APIQueryExternalBackupReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryExternalBackup"

	category "externalbackup"

	desc """查询外部备份"""

	rest {
		request {
			url "GET /v1/externalbackup"
			url "GET /v1/externalbackup/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryExternalBackupMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryExternalBackupReply.class
		}
	}
}