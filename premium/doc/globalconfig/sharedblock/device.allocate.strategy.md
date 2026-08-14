
## Name

```
device.allocate.strategy(落盘分配策略)
```

### Description

```
the strategy to allocate device for new shared block volume.
```

### 含义

```
控制共享快存储云盘创建时的落盘位置。
```

### Type

```
java.lang.String
```

### Category

```
sharedblock
```

### 取值范围

```
{none, minLvCounts, maxFreeSize}
```

### 取值范围补充说明

```
无，最少逻辑盘（云盘+快照）数量优先，最大剩余容量优先
```

### DefaultValue

```
none
```

### 默认值补充说明

```
按照 lvm 的默认策略，按盘符顺序分配
```

### 支持的资源级配置

||
|---|
|org.zstack.header.storage.primary.PrimaryStorageVO

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
