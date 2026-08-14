
## Name

```
vm.clock.sync.interval.in.seconds(VM定期时间同步时间间隔/单位:秒)
```

### Description

```
vm clock sync interval in seconds
```

### 含义

```
VM定期时间同步的时间间隔，以秒为单位
```

### Type

```
java.lang.Integer
```

### Category

```
vm
```

### 取值范围

```
{0, 60, 600, 1800, 3600, 7200, 21600, 43200, 86400}
```

### 取值范围补充说明

```
0表示不进行时间同步，其余表示以秒为单位进行同步
```

### DefaultValue

```
0
```

### 默认值补充说明

```
默认为0，定期时间同步关闭
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
