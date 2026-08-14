package org.zstack.drs.api

import org.zstack.drs.api.APIQueryDRSVmMigrationActivityReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryDRSVmMigrationActivity"

	category "drs"

	desc """查询集群DRS调度任务"""

	rest {
		request {
			url "GET /v1/clusters/drs/vm-migration-activities"
			url "GET /v1/clusters/drs/vm-migration-activities/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryDRSVmMigrationActivityMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryDRSVmMigrationActivityReply.class
		}
	}
}