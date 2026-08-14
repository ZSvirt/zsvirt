package org.zstack.directory

import org.zstack.directory.APIQueryDirectoryReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryDirectory"

	category "directory"

	desc """查询目录分组"""

	rest {
		request {
			url "GET /v1/directories"
			url "GET /v1/directories/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryDirectoryMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryDirectoryReply.class
		}
	}
}