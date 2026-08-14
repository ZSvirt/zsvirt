package org.zstack.nas

import org.zstack.nas.APIQueryNasFileSystemReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryNasFileSystem"

	category "nas.filesystem"

	desc """查询NAS文件系统"""

	rest {
		request {
			url "GET /v1/primary-storage/nas"
			url "GET /v1/primary-storage/nas/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryNasFileSystemMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryNasFileSystemReply.class
		}
	}
}