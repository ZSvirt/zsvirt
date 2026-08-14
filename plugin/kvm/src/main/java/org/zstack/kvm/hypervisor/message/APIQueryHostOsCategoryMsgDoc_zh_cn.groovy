package org.zstack.kvm.hypervisor.message

import org.zstack.kvm.hypervisor.message.APIQueryHostOsCategoryReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryHostOsCategory"

	category "host"

	desc """查询物理机系统类型"""

	rest {
		request {
			url "GET /v1/hosts/os/category"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryHostOsCategoryMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryHostOsCategoryReply.class
		}
	}
}