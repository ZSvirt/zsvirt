package org.zstack.storage.primary.sharedblock

import org.zstack.storage.primary.sharedblock.APIGetSharedBlockVolumeLanFreeLayoutReply

doc {
	title "GetSharedBlockVolumeLanFreeLayout"

	category "sharedblock"

	desc """获取共享块存储普通 Volume LV 的 LAN-free 物理布局，用于独立恢复目标投影。接口不激活并保持 LV，也不执行恢复；返回布局不代表 Volume 已从虚拟机及其他写入方隔离。恢复编排方必须在物理写入前保证目标 Volume 无并发写入，并在整个写入会话维持该排他条件。"""

	rest {
		request {
			url "POST /v1/primary-storage/sharedblockgroup/volume-lan-free-layouts"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetSharedBlockVolumeLanFreeLayoutMsg.class

			desc """volumeUuids 指定普通 Volume；单次最多 100 个且不能重复。"""

			params {

				column {
					name "volumeUuids"
					enclosedIn "params"
					desc "要查询的 Volume UUID 列表"
					location "body"
					type "List"
					optional false
					since "5.2.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "5.2.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "5.2.0"
				}
			}
		}

		response {
			clz APIGetSharedBlockVolumeLanFreeLayoutReply.class
		}
	}
}