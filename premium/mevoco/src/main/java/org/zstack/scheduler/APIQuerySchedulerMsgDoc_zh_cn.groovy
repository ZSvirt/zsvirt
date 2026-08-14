package org.zstack.scheduler

import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryScheduler"

    category "core.scheduler"

    desc """查询定时任务"""

    rest {
        request {
			url "GET /v1/schedulers"
			url "GET /v1/schedulers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIQuerySchedulerJobMsg.class

            desc """查询定时任务"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQuerySchedulerJobReply.class
        }
    }
}