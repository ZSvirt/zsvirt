package org.zstack.header.volume.block

import org.zstack.header.volume.block.APIQueryXskyBlockVolumeReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryXskyBlockVolume"

	category "mevoco"

	desc """查询xsky类型块存储卷返回"""

	rest {
		request {
			url "GET /v1/xksy/block-volumes"
			url "GET /v1/xsky/block-volumes/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryXskyBlockVolumeMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryXskyBlockVolumeReply.class
		}
	}
}