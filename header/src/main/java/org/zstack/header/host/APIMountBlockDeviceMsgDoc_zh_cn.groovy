package org.zstack.header.host

import org.zstack.header.host.APIMountBlockDeviceEvent

doc {
	title "MountBlockDevice"

	category "host"

	desc """挂载硬盘到挂载点"""

	rest {
		request {
			url "POST /v1/host/mount-block-device"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIMountBlockDeviceMsg.class

			desc """"""

			params {

				column {
					name "username"
					enclosedIn "params"
					desc "主机名"
					location "body"
					type "String"
					optional false
					since "zsv 4.3.0"
				}
				column {
					name "password"
					enclosedIn "params"
					desc "主机密码"
					location "body"
					type "String"
					optional true
					since "zsv 4.3.0"
				}
				column {
					name "sshPort"
					enclosedIn "params"
					desc "主机端口"
					location "body"
					type "Integer"
					optional false
					since "zsv 4.3.0"
				}
				column {
					name "hostName"
					enclosedIn "params"
					desc "主机IP"
					location "body"
					type "String"
					optional false
					since "zsv 4.3.0"
				}
				column {
					name "path"
					enclosedIn "params"
					desc "硬盘绝对路径(举例：'/dev/vdb')"
					location "body"
					type "String"
					optional false
					since "zsv 4.3.0"
				}
				column {
					name "mountPoint"
					enclosedIn "params"
					desc "挂载点"
					location "body"
					type "String"
					optional false
					since "zsv 4.3.0"
				}
				column {
					name "filesystemType"
					enclosedIn "params"
					desc "文件系统类型"
					location "body"
					type "String"
					optional true
					since "zsv 4.3.0"
					values ("ext4","xfs")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.3.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.3.0"
				}
			}
		}

		response {
			clz APIMountBlockDeviceEvent.class
		}
	}
}