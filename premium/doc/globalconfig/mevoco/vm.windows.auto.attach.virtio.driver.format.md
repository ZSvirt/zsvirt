
## Name

```
vm.windows.auto.attach.virtio.driver.format(Windows VM 自动加载 VirtIO 驱动的类型)
```

### Description

```
the format of the virtio dirver installer is attached to a new created windows virtio vm
```

### 含义

```
Windows VM 自动加载 VirtIO 驱动的类型，默认 "vfd" 表示加载格式为 VFD 的 VirtIO 驱动给 Windows 云主机
```

### Type

```
java.lang.String
```

### Category

```
mevoco
```

### 取值范围

```
{NONE, ISO, VFD}
```

### 取值范围补充说明

```
##对取值范围的解读-如无需写：无##
```

### DefaultValue

```
VFD
```

### 默认值补充说明

```
默认 "VFD" 表示加载格式为 VFD 的 VirtIO 驱动给 Windows 云主机，
"NONE" 表示禁用该功能，将不会自动添加 VirtIO 驱动给 Windows 云主机，
"ISO"，表示加载的是格式为 ISO 的 VirtIO 驱动给 Windows 云主机。

需要注意的是，会加载驱动的只有 Windows 的、以 VirtIO ISO 镜像启动的、刚创建的云主机。
其它云主机、重启过的云主机均不会添加 VirtIO 驱动的软驱或光驱。
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
无
```

### UI暴露

```
否
```

### CLI手册暴露

```
否
```

## 注意事项

```
无
```
