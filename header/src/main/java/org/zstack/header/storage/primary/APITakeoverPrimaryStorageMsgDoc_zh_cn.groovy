package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APITakeoverPrimaryStorageEvent

doc {
	title "TakeoverPrimaryStorage"

	category "storage.primary"

	desc """接管主存储。将其他ZStack平台的共享块主存储接管到当前平台。
dryRun=true时仅执行一致性预检：若一致（无需接管）则返回错误，若不一致（可接管）则返回成功。
接管操作不可逆（agent侧会执行VG rename、PV UUID reset、sanlock lockspace reset）。
接管完成后自动触发reconnect，通过返回的inventory.status获取重连状态。"""

	rest {
		request {
			url "PUT /v1/primary-storage/{uuid}/takeover"

			header (Authorization: 'OAuth the-session-uuid')

			clz APITakeoverPrimaryStorageMsg.class

			desc """接管指定主存储"""

			params {

				column {
					name "uuid"
					enclosedIn "takeoverPrimaryStorage"
					desc "主存储的UUID"
					location "url"
					type "String"
					optional false
					since "zsv 5.0.0"
				}
				column {
					name "dryRun"
					enclosedIn "takeoverPrimaryStorage"
					desc "预检模式：成功表示可以执行接管，失败表示无需或无法接管"
					location "body"
					type "boolean"
					optional true
					since "zsv 5.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 5.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 5.0.0"
				}
			}
		}

		response {
			clz APITakeoverPrimaryStorageEvent.class
		}
	}
}