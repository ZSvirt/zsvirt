
## Name

```
vm.hyperv.clock.feature(云主机hypervclock特性)
```

### Description

```
Enable or disable hypervclock
```

### 含义

```
控制启用和禁用云主机的hypervclock特性
```

### Type

```
java.lang.Boolean
```

### Category

```
kvm
```

### 取值范围

```
{true, false}
```

### 取值范围补充说明

```
true是启用hypervclock特性

false是禁用hypervclock特性
```

### DefaultValue

```
true
```

### 默认值补充说明

```
默认启用，保持原本的云主机行为
```

### 支持的资源级配置

||
|---|
|org.zstack.header.vm.VmInstanceVO

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
暴露
```

### CLI手册暴露

```
暴露
```

## 注意事项

```
这个配置针对windows云主机生效，并且不要求开启云主机hyper-v特性，仅优化云主机时钟
```
