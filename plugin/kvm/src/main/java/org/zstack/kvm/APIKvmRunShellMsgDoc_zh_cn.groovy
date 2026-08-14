package org.zstack.kvm

import org.zstack.kvm.APIKvmRunShellEvent

doc {
	title "KvmRunShell"

	category "host"

	desc """KVM运行命令"""

	rest {
		request {
			url "PUT /v1/hosts/kvm/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIKvmRunShellMsg.class

			desc """KVM运行命令"""

			params {

				column {
					name "hostUuids"
					enclosedIn "kvmRunShell"
					desc "目标机器UUID"
					location "body"
					type "Set"
					optional false
					since "0.6"
				}
				column {
					name "script"
					enclosedIn "kvmRunShell"
					desc "脚本"
					location "body"
					type "String"
					optional false
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
			clz APIKvmRunShellEvent.class
		}
	}
}