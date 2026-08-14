package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APIQueryImageCacheReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryImageCache"

	category "storage.primary"

	desc """查询主存储上的镜像缓存"""

	rest {
		request {
			url "GET /v1/primary-storage/imagecache"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryImageCacheMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryImageCacheReply.class
		}
	}
}