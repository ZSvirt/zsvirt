package org.zstack.ovf.api

import org.zstack.ovf.api.APIQueryImagePackageReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryImagePackage"

	category "ovf"

	desc """查询镜像包"""

	rest {
		request {
			url "GET /v1/image-packages"
			url "GET /v1/image-packages/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryImagePackageMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryImagePackageReply.class
		}
	}
}