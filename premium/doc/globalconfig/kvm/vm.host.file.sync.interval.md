
## Name

```
vm.host.file.sync.interval(主机侧虚拟机文件检查间隔)
```

### Description

```
Interval in seconds for checking VM host files (NvRam, TpmState) on KVM hosts
```

### 含义

```
KVM 主机侧虚拟机文件（一般是 NvRam 或者 Tpm 文件）检查间隔，单位：秒
当检查到虚拟机文件变动时，会同步一次文件数据至管理节点数据库。
当多次发现虚拟机文件没有变动时，也会进行强制同步
```

### Type

```
java.lang.Long
```

### Category

```
kvm
```

### 取值范围

```
[1, 86400]
```

### 取值范围补充说明

```
最小间隔 1 秒，最大间隔 1 天
```

### DefaultValue

```
15
```

### 默认值补充说明

```
默认间隔 15 秒
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
不暴露
```

### CLI手册暴露

```
暴露
```

## 注意事项

```
无
```
