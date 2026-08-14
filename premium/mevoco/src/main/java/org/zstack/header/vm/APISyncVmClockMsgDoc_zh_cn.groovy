package org.zstack.header.vm

import org.zstack.header.vm.APISyncVmClockEvent

doc {
	title "SyncVmClock"

	category "mevoco"

	desc """立即同步云主机时钟"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISyncVmClockMsg.class

			desc """立即将指定云主机和所在物理机的时钟进行同步，仅对已经安装了 QEMU Guest Agent 的云主机有效"""

			params {

				column {
					name "uuid"
					enclosedIn "syncVmClock"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.14.12"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.14.12"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.14.12"
				}
			}
		}

		response {
			clz APISyncVmClockEvent.class
		}
	}
}