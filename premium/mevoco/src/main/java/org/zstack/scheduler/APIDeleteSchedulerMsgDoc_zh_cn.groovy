package org.zstack.scheduler

doc {
    title "DeleteScheduler"

    category "core.scheduler"

    desc """删除定时任务"""

    rest {
        request {
			url "DELETE /v1/schedulers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIDeleteSchedulerJobMsg.class

            desc """删除定时任务"""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "设置为Permissive时, 如果删除过程中发生错误或者删除不被允许ZStack会停止删除操作; 在这种情况下, 包含失败原因的错误代码会被返回;设置为Enforcing, ZStack会忽略所有错误和权限而直接删除资源; 在这种情况下, 删除操作总是会成功"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIDeleteSchedulerJobEvent.class
        }
    }
}