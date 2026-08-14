package org.zstack.header.volume.block

import org.zstack.header.volume.block.APIQueryBlockVolumeReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryBlockVolume"

	category "mevoco"

	desc """查询块存储卷"""

	rest {
		request {
			url "GET /v1/block-volumes"
			url "GET /v1/block-volumes/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryBlockVolumeMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryBlockVolumeReply.class
		}
	}
}