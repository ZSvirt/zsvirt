package org.zstack.kvm.hypervisor.message

import org.zstack.kvm.hypervisor.message.APIQueryKvmHypervisorInfoReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryKvmHypervisorInfo"

	category "host"

	desc """查询资源的虚拟化软件信息"""

	rest {
		request {
			url "GET /v1/hosts/kvm/hypervisor/info"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryKvmHypervisorInfoMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryKvmHypervisorInfoReply.class
		}
	}
}