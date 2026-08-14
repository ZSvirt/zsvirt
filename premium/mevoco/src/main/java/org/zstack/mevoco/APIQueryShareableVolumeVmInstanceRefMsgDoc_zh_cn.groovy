package org.zstack.mevoco

import org.zstack.mevoco.APIQueryShareableVolumeVmInstanceRefReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询共享磁盘所挂载的云主机"

	category "mevoco"

	desc """查询共享磁盘所挂载的云主机"""

	rest {
		request {
			url "GET /v1/volumes/vm-instances/refs"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryShareableVolumeVmInstanceRefMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryShareableVolumeVmInstanceRefReply.class
		}
	}
}