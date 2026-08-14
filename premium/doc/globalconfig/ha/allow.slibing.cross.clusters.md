
## Name

```
allow.slibing.cross.clusters(##主机跨集群探测##)
```

### Description

```
Allow the use of other hosts with the same storage but different clusters to check if the target host is still connected.
```

### 含义

```
允许使用同存储但不同集群的其它主机，来检查目标主机是否还连接。
一般在目标主机失联时会用到。
```

### Type

```
java.lang.Boolean
```

### Category

```
ha
```

### 取值范围

```
[-9223372036854775808, 9223372036854775807]
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
在 ZSphere 4.10.6 之前，默认值为 false。
当整个集群的主机挂掉后，因为其它集群的主机无法探测目标集群的主机，即使跨集群迁移的配置打开，
也仍然无法 HA 启动目标集群中的虚拟机。

在 ZSphere 4.10.6 及以后，改默认值被修改为 true。
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
请查看上面的【默认值补充说明】章节
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
