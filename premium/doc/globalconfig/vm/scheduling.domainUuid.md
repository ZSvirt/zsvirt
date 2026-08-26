
## Name

```
scheduling.domainUuid(云主机调度域)
```

### Description

```
Configured allocator domain uuid for a VM. None means no domain. This is the desired domain, not the currently effective domain.
```

### 含义

```
配置云主机期望使用的调度域 UUID。None 表示未指定域。
这是配置值，不是当前生效的域；当前值写在 SystemTag 上。
```

### Type

```
java.lang.String
```

### Category

```
vm
```

### 取值范围

```
[-9223372036854775808, 9223372036854775807]
```

### 取值范围补充说明

```
None 表示未指定域；否则为调度域 UUID。不允许空串。
```

### DefaultValue

```
None
```

### 默认值补充说明

```
默认为 None，表示未指定调度域。
```

### 支持的资源级配置

||
|---|
|org.zstack.header.vm.VmInstanceVO

### 资源粒度说明

```
支持云主机粒度
```

### 背景信息

```
ZSV-13008 调度策略：创建、关机后开机读配置值；热迁移、挂盘、挂网、HA、主机维护读 SystemTag 当前值。
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
GlobalConfig / ResourceConfig 不允许值为空串，未指定域必须写 None。
一期 APIUpdateResourceConfig 只能设为 None 或幂等写回相同域 UUID，不能改到另一个域；DeleteResourceConfig 同等放行。
```
