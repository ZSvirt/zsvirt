
## Name

```
enable.vm.migration.host.cpu.function.check(热迁移CPU检测开关)
```

### Description

```
Check whether the CPU function of the dstHost is compatible with the CPU function of the srcHost.
```

### 含义

```
开启该功能将会在云主机进行热迁移的时候会筛掉不满足条件的目的物理机列表
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
无
```

### DefaultValue

```
true
```

### 默认值补充说明

```
无
```

### 支持的资源级配置



### 资源粒度说明

```
全局
```

### 背景信息

```
为了防止迁移云主机到不满足条件的目的物理机上，云主机会变成暂停状态
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
无
```
