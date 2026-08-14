package org.zstack.nas

import org.zstack.nas.APIQueryNasMountTargetReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryNasMountTarget"

	category "nas.filesystem"

	desc """查询NAS挂载点信息"""

	rest {
		request {
			url "GET /v1/primary-storage/nas/mount"
			url "GET /v1/primary-storage/nas/mount/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryNasMountTargetMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryNasMountTargetReply.class
		}
	}
}