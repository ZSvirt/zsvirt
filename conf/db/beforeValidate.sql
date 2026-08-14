-- Update `schema_version' since we've changed SQL to match the expectation
-- of the newer version MariaDB.
--
-- WARNING: `schema_version' table doesn't exist on a clean environment, thus
-- after 'flyway clean' we prepared an empty table after 'flyway baseline'.
DELIMITER $$

DROP PROCEDURE IF EXISTS `zstack`.`update_schema_checksum` $$

-- CREATE PROCEDURE `zstack`.`update_schema_checksum`()
-- BEGIN
--     IF EXISTS(SELECT table_name FROM information_schema.tables WHERE table_name = 'schema_version')
--     THEN
--         update `zstack`.`schema_version` set `checksum`=1083194846  where `script`='V1.6__schema.sql'   and `checksum` <> 1083194846;
--     END IF;
-- END $$

DELIMITER  ;

-- CALL `zstack`.update_schema_checksum();
-- DROP PROCEDURE IF EXISTS `zstack`.update_schema_checksum;
