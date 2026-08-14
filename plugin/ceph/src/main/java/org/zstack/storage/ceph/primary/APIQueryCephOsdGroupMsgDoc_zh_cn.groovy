package org.zstack.storage.ceph.primary

import org.zstack.storage.ceph.primary.APIQueryCephOsdGroupReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryCephOsdGroup"

	category "未知类别"

	desc """查询CephOsdGroup相关信息"""

	rest {
		request {
			url "GET /v1/primary-storage/ceph/osdgroups"
			url "GET /v1/primary-storage/ceph/osdgroups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryCephOsdGroupMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryCephOsdGroupReply.class
		}
	}
}