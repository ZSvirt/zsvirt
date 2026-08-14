package org.zstack.header.vm.cdrom

import org.zstack.header.vm.cdrom.APIQueryVmCdRomReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryVmCdRom"

	category "vmInstance"

	desc """查询CDROM清单"""

	rest {
		request {
			url "GET /v1/vm-instances/cdroms"
			url "GET /v1/vm-instances/cdroms/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVmCdRomMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVmCdRomReply.class
		}
	}
}