package org.zstack.header.image

import org.zstack.header.image.APIUpdateImageEvent

doc {
	title "更新镜像信息(UpdateImage)"

	category "image"

	desc """更新镜像信息"""

	rest {
		request {
			url "PUT /v1/images/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateImageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateImage"
					desc "镜像的UUID，唯一标示该镜像"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "name"
					enclosedIn "updateImage"
					desc "镜像名称"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "updateImage"
					desc "镜像的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "guestOsType"
					enclosedIn "updateImage"
					desc "镜像对应的客户机操作系统类型"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "mediaType"
					enclosedIn "updateImage"
					desc "镜像的类型"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
					values ("RootVolumeTemplate","DataVolumeTemplate","ISO")
				}
				column {
					name "format"
					enclosedIn "updateImage"
					desc "镜像的格式"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
					values ("raw","qcow2","iso")
				}
				column {
					name "system"
					enclosedIn "updateImage"
					desc "标识是否为系统镜像"
					location "body"
					type "Boolean"
					optional true
					since "zsv 0.6"
				}
				column {
					name "platform"
					enclosedIn "updateImage"
					desc "镜像的系统平台"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
					values ("Linux","Windows","Other","Paravirtualization","WindowsVirtio")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "architecture"
					enclosedIn "updateImage"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 3.10"
					values ("x86_64","aarch64","mips64el","loongarch64")
				}
				column {
					name "virtio"
					enclosedIn "updateImage"
					desc ""
					location "body"
					type "Boolean"
					optional true
					since "zsv 3.11.2"
				}
			}
		}

		response {
			clz APIUpdateImageEvent.class
		}
	}
}