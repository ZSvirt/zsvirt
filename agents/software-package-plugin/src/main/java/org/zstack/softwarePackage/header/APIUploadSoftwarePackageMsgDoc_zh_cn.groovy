package org.zstack.softwarePackage.header

doc {
	title "UploadSoftwarePackage"

	category "softwarePackage"

	desc """上传软件包"""

	rest {
		request {
			url "POST /v1/software-packages/upload"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUploadSoftwarePackageMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "软件包名"
					location "body"
					type "String"
					optional false
					since "4.10.20"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "软件包类型"
					location "body"
					type "String"
					optional false
					since "4.10.20"
				}
				column {
					name "managementNodeUuid"
					enclosedIn "params"
					desc "上传到管理节点UUID"
					location "body"
					type "String"
					optional false
					since "4.10.20"
				}
				column {
					name "hostUuid"
					enclosedIn "params"
					desc "上传到主机UUID"
					location "body"
					type "String"
					optional false
					since "4.10.20"
				}
				column {
					name "url"
					enclosedIn "params"
					desc "软件包URL"
					location "body"
					type "String"
					optional false
					since "4.10.20"
				}
				column {
					name "installPath"
					enclosedIn "params"
					desc "软件包安装路径"
					location "body"
					type "String"
					optional false
					since "4.10.20"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "4.10.20"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "4.10.20"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.10.20"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.10.20"
				}
			}
		}

		response {
			clz APIUploadSoftwarePackageEvent.class
		}
	}
}