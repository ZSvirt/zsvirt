
## Name

```
enable.uefi.secure.boot(启动UEFI SECURE BOOT)
```

### Description

```
enable uefi secure boot
```

### 含义

```
启动UEFI SECURE BOOT
```

### Type

```
java.lang.Boolean
```

### Category

```
vm
```

### 取值范围

```
{true, false}
```

### 取值范围补充说明

```
无
```

### DefaultValue

```
false
```

### 默认值补充说明

```
在 zsv_5.0.0 之前, 默认值是 true, 但这个值没有实际效果。
在 zsv_5.0.0 之后, 默认值修改为 false, 就是怕它升级后, 原本 UEFI 的虚拟机因为配置修改导致无法启动。
```

### 支持的资源级配置

||
|---|
|org.zstack.header.vm.VmInstanceVO
|org.zstack.header.cluster.ClusterVO

### 资源粒度说明

```
从 5.0.0 版本后开始支持资源级配置
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
