use zstack_billing;

ALTER TABLE zstack_billing.vmusageco DROP cpuprice;
ALTER TABLE zstack_billing.vmusageco DROP memoryprice;
ALTER TABLE zstack_billing.vmusageco DROP volumeprice;
ALTER TABLE zstack_billing.datavolumeusageco DROP price;
ALTER TABLE zstack_billing.rootvolumeusageco DROP price;

DROP TYPE IF EXISTS PriceUDF;

INSERT INTO VersionCO (name, version, schemaName, date) VALUES('schema_version', '1.4', 'V1.4__schema.cql', dateof(now()));
