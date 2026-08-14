
## Name

```
trash.expiration.time(回收站过期时间)
```

### Description

```
Volume that have been in trash more than expiration time in seconds will be automatically deleted.
```

### 含义

```
在回收站中的卷超过过期时间（秒）将自动删除。
```

### Type

```
java.lang.Integer
```

### Category

```
primaryStorage
```

### 取值范围

```
[0, 2147483647]
```

### 取值范围补充说明

```
单位为秒，0表示资源会立即删除，不会进入回收站。
```

### DefaultValue

```
604800
```

### 默认值补充说明

```
默认为7天
```

### 支持的资源级配置

||
|---|
|org.zstack.header.storage.primary.PrimaryStorageVO

### 资源粒度说明

```
该配置作用于主存储
```

### 背景信息

```
回收站用于防止误删导致的数据丢失，过期时间内可联系技术支持人员恢复数据，过期后数据将被永久删除。
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
如果对应的主存储不支持秒级别的过期时间，将会用进一法转换为对应的最小单位。
```
