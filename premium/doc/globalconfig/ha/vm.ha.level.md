
## Name

```
vm.ha.level(默认云主机高可用级别)
```

### Description

```
The default High-Availability (HA) state of a newly created VM
```

### 含义

```
新创建的虚拟机默认的高可用状态
```

### Type

```
java.lang.String
```

### Category

```
ha
```

### 取值范围

```
{NeverStop, OnHostFailure, FaultTolerance, None}
```

### 取值范围补充说明

```
ZSphere 仅支持 None 和 NeverStop
```

### DefaultValue

```
None
```

### 默认值补充说明

```
无
```

### 支持的资源级配置

||
|---|
|org.zstack.header.cluster.ClusterVO

### 资源粒度说明

```
VmInstanceVO
```

### 背景信息

```
项目需求
```

### UI暴露

```
是
```

### CLI手册暴露

```
是
```

## 注意事项

```
该配置仅对新创建的虚拟机生效。

当虚拟机创建完成之后，它是否启用高可用和该配置无关。
```
