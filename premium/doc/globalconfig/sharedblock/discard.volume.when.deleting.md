
## Name

```
discard.volume.when.deleting(当删除云盘时发起数据丢弃)
```

### Description

```
always means always issuing discards to PVs when deleting LV, never means not issuing discards and auto means that if discards are relatively fast, then discards will be issued.
```

### 含义

```
在删除逻辑卷时向所在的lun发起数据丢弃的策略
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
{always, never, auto}
```

### 取值范围补充说明

```
always表示逻辑卷在删除时需要向所在的lun发起数据丢弃，never从不发起数据丢弃，auto表示如果丢弃速度相对较快才发起丢弃
```

### DefaultValue

```
never
```

### 默认值补充说明

```
默认值是never，表示逻辑卷在删除时从不向所在的lun发起数据丢弃
```

### 支持的资源级配置

||
|---|
|org.zstack.header.storage.primary.PrimaryStorageVO

### 资源粒度说明

```
默认是针对平台内所有sharedblock主存储生效，可通过资源配置单独配置某个sharedblock主存储的数据丢弃策略
```

### 背景信息

```
##触发该条目增删改的背景-如无需写：无##
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
如果启用discard功能，且存储本身支持blkdiscard，那么删除云盘后，上面的数据将会置零且永久丢失，如果存储不支持blkdiscard，那么开启用此功能无效。
```
