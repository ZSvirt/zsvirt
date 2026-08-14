package org.zstack.header.vm

import org.zstack.header.vm.APIQueryTemplatedVmInstanceReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryTemplatedVmInstance"

	category "vmInstance"

	desc """查询虚拟机模板"""

	rest {
		request {
			url "GET /v1/vm-instances/templatedVmInstance"
			url "GET /v1/vm-instances/templatedVmInstance/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryTemplatedVmInstanceMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryTemplatedVmInstanceReply.class
		}
	}
}