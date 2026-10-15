## Name

```
vddk.version.md5(ZMigrate所需VDDK版本及MD5)
```

### Description

```
required ZMigrate VDDK versions and MD5 checksums
```

### 含义

```
ZMigrate管理虚拟机必须同时具备的两个VDDK版本及其MD5。格式为version-md5;version-md5，文件名不参与版本识别。
```

### Type

```
java.lang.String
```

### Category

```
zmigrate
```

### 取值范围

```
无
```

### DefaultValue

```
6.7.3-6d28fff82e478958e5215d14a7ec8573;8.0.3-007ab979e52f52401f02278b75ab5c74
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
上传时必须同时包含两个版本，且校验和完全匹配；上传、校验和安装成功后，系统通过ZMigrate管理虚拟机上的成功标记返回APIGetZMigrateInfos.vddkUploaded=true。
```
