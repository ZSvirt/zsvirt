package org.zstack.header.volume.block

import org.zstack.header.volume.block.APIQueryExponBlockVolumeReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryExponBlockVolume"

	category "mevoco"

	desc """查询expon类型块存储卷"""

	rest {
		request {
			url "GET /v1/expon/block-volumes"
			url "GET /v1/expon/block-volumes/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryExponBlockVolumeMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryExponBlockVolumeReply.class
		}
	}
}