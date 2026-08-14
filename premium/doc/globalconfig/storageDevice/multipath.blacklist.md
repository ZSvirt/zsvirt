
## Name

```
multipath.blacklist(多路径服务的黑名单配置)
```

### Description

```
blacklist of multipath, for example: [{device:[{vendor:"IBM"}, {product:"3S42"}]}, {wwid:"36001405913ad48768b84db39bbcc5cb0"}], and empty string means to make no changes to the current configuration
```

### 含义

```
用于配置多路径服务的黑名单列表
```

### Type

```
java.lang.String
```

### Category

```
storageDevice
```

### 取值范围

```

```

### 取值范围补充说明

```
无
```

### DefaultValue

```

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
是
```

## 注意事项

```
修改全局配置或者资源配置后需要重连物理机使配置生效
```
