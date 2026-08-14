use zstack;

DELIMITER $$

DROP FUNCTION IF EXISTS `Json_getKeyValue` $$

CREATE FUNCTION `Json_getKeyValue`(
    in_JsonArray VARCHAR(4096),
    in_KeyName VARCHAR(64)
) RETURNS VARCHAR(4096) CHARSET utf8

BEGIN
    DECLARE vs_return, vs_JsonArray, vs_JsonString, vs_Json, vs_KeyName VARCHAR(4096);
    DECLARE vi_pos1, vi_pos2 SMALLINT UNSIGNED;

    SET vs_JsonArray = TRIM(in_JsonArray);
    SET vs_KeyName = TRIM(in_KeyName);

    IF vs_JsonArray = '' OR vs_JsonArray IS NULL
        OR vs_KeyName = '' OR vs_KeyName IS NULL
    THEN
        SET vs_return = NULL;
    ELSE
        SET vs_JsonArray = REPLACE(REPLACE(vs_JsonArray, '[', ''), ']', '');
        SET vs_JsonString = CONCAT("'", vs_JsonArray, "'");
        SET vs_json = SUBSTRING_INDEX(SUBSTRING_INDEX(vs_JsonString,'}',1), '{', -1);

        IF vs_json = '' OR vs_json IS NULL THEN
            SET vs_return = NULL;
        ELSE
            SET vs_KeyName = CONCAT('"', vs_KeyName, '":');
            SET vi_pos1 = INSTR(vs_json, vs_KeyName);

            IF vi_pos1 > 0 THEN
                SET vi_pos1 = vi_pos1 + CHAR_LENGTH(vs_KeyName);
                SET vi_pos2 = LOCATE('","', vs_json, vi_pos1);

                IF vi_pos2 = 0 THEN
                    SET vi_pos2 = CHAR_LENGTH(vs_json) + 1;
                END IF;

            SET vs_return = REPLACE(MID(vs_json, vi_pos1, vi_pos2 - vi_pos1), '"', '');
            END IF;
        END IF;
    END IF;
    RETURN(vs_return);
END$$
DELIMITER  ;

SET FOREIGN_KEY_CHECKS = 0;

DROP PROCEDURE IF EXISTS cleanupUsedIpVO;
DELIMITER $$
CREATE PROCEDURE cleanupUsedIpVO()
BEGIN
    DECLARE curUsedIpUuid VARCHAR(32);
    DECLARE vipCount INT DEFAULT 0;
    DECLARE vmNicCount INT DEFAULT 0;
    DECLARE dhcpCount INT DEFAULT 0;
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT uuid FROM `zstack`.`UsedIpVO` usedIp;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO curUsedIpUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SELECT COUNT(*) INTO vipCount FROM VipVO WHERE usedIpUuid = curUsedIpUuid;
        IF (vipCount > 0) THEN
            ITERATE read_loop;
        END IF;

        SELECT COUNT(*) INTO vmNicCount FROM VmNicVO WHERE usedIpUuid = curUsedIpUuid;
        IF (vmNicCount > 0) THEN
            ITERATE read_loop;
        END IF;

        SELECT COUNT(*) INTO dhcpCount FROM SystemTagVO WHERE resourceType='L3NetworkVO'
         AND tag LIKE CONCAT('flatNetwork::DhcpServer::%::ipUuid::', curUsedIpUuid,  '%');
        IF (dhcpCount > 0) THEN
            ITERATE read_loop;
        END IF;

        DELETE FROM UsedIpVO WHERE uuid = curUsedIpUuid;
    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;

DROP PROCEDURE IF EXISTS `DELETE_INDEX`;

DELIMITER $$
CREATE PROCEDURE `DELETE_INDEX`(
    IN tb_name VARCHAR(64),
    IN idx_name VARCHAR(64)
)
DETERMINISTIC
READS SQL DATA
begin_label: BEGIN
    IF idx_name = '' OR idx_name IS NULL THEN
        LEAVE begin_label;
    END IF;

    IF EXISTS ( SELECT * FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                AND table_name = tb_name
                AND index_name = idx_name ) THEN
        SET @sql = CONCAT('ALTER TABLE ', tb_name, ' DROP INDEX ', idx_name);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
    END IF;

    SELECT CURTIME();
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS `CREATE_INDEX`;

DELIMITER $$
CREATE PROCEDURE `CREATE_INDEX`(
    IN tb_name VARCHAR(64),
    IN idx_name VARCHAR(64),
    IN col_name VARCHAR(64)
)
    DETERMINISTIC
    READS SQL DATA
begin_label: BEGIN
    IF idx_name = '' OR idx_name IS NULL THEN
        LEAVE begin_label;
    END IF;

    IF NOT EXISTS ( SELECT * FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = tb_name
                  AND index_name = idx_name ) THEN
        SET @sql = CONCAT('ALTER TABLE ', tb_name, ' ADD INDEX ', idx_name, ' (`', col_name, '`)');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
    END IF;

    SELECT CURTIME();
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS `ADD_COLUMN`;

DELIMITER $$
CREATE PROCEDURE `ADD_COLUMN`(
    IN tb_name VARCHAR(64),
    IN col_name VARCHAR(64),
    IN col_data_type VARCHAR(64),
    IN allow_null BOOL,
    IN default_value VARCHAR(255)
)
BEGIN
    DECLARE alter_sql VARCHAR(1000);
    IF NOT EXISTS( SELECT 1
                           FROM INFORMATION_SCHEMA.COLUMNS
                           WHERE table_name = tb_name
                                AND table_schema = 'zstack'
                                AND column_name = col_name) THEN
                SET @alter_sql = CONCAT('ALTER TABLE zstack.', tb_name, ' ADD COLUMN ', col_name, ' ', col_data_type);
                IF NOT allow_null THEN
                    SET @alter_sql = CONCAT(@alter_sql, ' NOT NULL');
                END IF;
                IF default_value IS NOT NULL THEN
                          SET @alter_sql = CONCAT(@alter_sql, ' DEFAULT ''', default_value, '''');
                ELSE
                          SET @alter_sql = CONCAT(@alter_sql, ' DEFAULT NULL');
                END IF;
                SELECT @alter_sql;
                PREPARE stmt FROM @alter_sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
    END IF;

SELECT CURTIME();
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS `ADD_CONSTRAINT`;

DELIMITER $$
CREATE PROCEDURE `ADD_CONSTRAINT`(
    IN tb_name VARCHAR(64),
    IN cons_name VARCHAR(255),
    IN references_col VARCHAR(64),
    IN referencing_table VARCHAR(64),
    IN referencing_col VARCHAR(64),
    IN on_delete VARCHAR(64)
)
BEGIN
    DECLARE alter_sql VARCHAR(1000);
    IF NOT EXISTS( SELECT 1
                           FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                           WHERE table_name = tb_name
                                AND table_schema = 'zstack'
                                AND CONSTRAINT_NAME = cons_name) THEN
                SET @alter_sql = CONCAT('ALTER TABLE zstack.', tb_name, ' ADD CONSTRAINT ', cons_name,
                        ' FOREIGN KEY (', references_col, ') REFERENCES ', 'zstack.', referencing_table, ' (', referencing_col,
                        ') ON DELETE ', on_delete);
                SELECT @alter_sql;
                PREPARE stmt FROM @alter_sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
    END IF;

SELECT CURTIME();
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS `DROP_COLUMN`;
DELIMITER $$
CREATE PROCEDURE `DROP_COLUMN`(
    IN tb_name VARCHAR(64),
    IN col_name VARCHAR(64)
)
BEGIN
    DECLARE alter_sql VARCHAR(1000);
    IF EXISTS( SELECT NULL
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE table_name = tb_name
            AND table_schema = 'zstack'
            AND column_name = col_name) THEN
        SET @alter_sql = CONCAT('ALTER TABLE zstack.', tb_name, ' DROP ', col_name);
        SELECT @alter_sql;
        PREPARE stmt FROM @alter_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

SELECT CURTIME();
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS `INSERT_COLUMN`;

DELIMITER $$
CREATE PROCEDURE `INSERT_COLUMN`(
    IN tb_name VARCHAR(64),
    IN col_name VARCHAR(64),
    IN col_data_type VARCHAR(64),
    IN allow_null BOOL,
    IN default_value VARCHAR(255),
    IN col_name_to_after VARCHAR(64)
)
BEGIN
    DECLARE alter_sql VARCHAR(1000);
    IF NOT EXISTS( SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE table_name = tb_name
            AND table_schema = 'zstack'
            AND column_name = col_name) THEN
        SET @alter_sql = CONCAT('ALTER TABLE zstack.', tb_name, ' ADD COLUMN ', col_name, ' ', col_data_type);
        IF NOT allow_null THEN
            SET @alter_sql = CONCAT(@alter_sql, ' NOT NULL');
        END IF;
        IF default_value IS NOT NULL THEN
            SET @alter_sql = CONCAT(@alter_sql, ' DEFAULT ''', default_value, '''');
        ELSE
            SET @alter_sql = CONCAT(@alter_sql, ' DEFAULT NULL');
        END IF;
        IF col_name_to_after IS NOT NULL THEN
            SET @alter_sql = CONCAT(@alter_sql, ' AFTER ', col_name_to_after);
        END IF;
        SELECT @alter_sql;
        PREPARE stmt FROM @alter_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

SELECT CURTIME();
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS `RENAME_TABLE`;
DELIMITER $$
CREATE PROCEDURE `RENAME_TABLE`(
    IN old_name VARCHAR(64),
    IN new_name VARCHAR(64)
)
BEGIN
    DECLARE ALTER_SQL VARCHAR(1000);
    IF EXISTS (SELECT * FROM information_schema.tables WHERE table_name = old_name AND table_schema = 'zstack') THEN
        SET @alter_sql = CONCAT('RENAME TABLE zstack.', old_name, ' TO ', new_name);
        SELECT @alter_sql;
        PREPARE stmt FROM @alter_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
    SELECT CURTIME();
END $$
DELIMITER ;

DROP PROCEDURE IF EXISTS `DROP_FOREIGN_KEY`;
DELIMITER $$
CREATE PROCEDURE `DROP_FOREIGN_KEY`(
    IN tb_name VARCHAR(64),
    IN fk_name VARCHAR(64)
)
BEGIN
    DECLARE alter_sql VARCHAR(1000);
    IF EXISTS(SELECT * FROM information_schema.table_constraints
            WHERE table_name = tb_name
            AND table_schema = 'zstack'
            AND constraint_name = fk_name) THEN
        SET @alter_sql = CONCAT('ALTER TABLE zstack.', tb_name, ' DROP FOREIGN KEY ', fk_name);
        SELECT @alter_sql;
        PREPARE stmt FROM @alter_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
    SELECT CURTIME();
END $$
DELIMITER ;

--/*
-- * Generic procedure to safely add foreign key constraints
-- * Specifically designed for the 'zstack' database
-- *
-- * Parameters:
-- *   table_name:       Table receiving foreign key constraint
-- *   constraint_name:  Name for new foreign key constraint
-- *   column_name:      Column acting as foreign key
-- *   ref_table:        Referenced table (parent table)
-- *   ref_column:       Referenced column (typically primary key)
-- *   on_delete_action: ON DELETE action (CASCADE, RESTRICT, etc)
-- */
DROP PROCEDURE IF EXISTS `GENERIC_ADD_FOREIGN_KEY`;
DELIMITER $$
CREATE PROCEDURE `GENERIC_ADD_FOREIGN_KEY`(
    IN table_name VARCHAR(64),
    IN constraint_name VARCHAR(64),
    IN column_name VARCHAR(64),
    IN ref_table VARCHAR(64),
    IN ref_column VARCHAR(64),
    IN on_delete_action VARCHAR(20)
)
BEGIN
    DECLARE constraint_exists INT DEFAULT 0;
    DECLARE table_exists INT DEFAULT 0;

    -- Verify target table exists in zstack database
    SELECT COUNT(*) INTO table_exists
    FROM information_schema.tables
    WHERE table_schema = 'zstack'
    AND table_name = table_name;

    -- Only proceed if target table exists
    IF table_exists > 0 THEN
        -- Check if constraint already exists to prevent duplicates
        SELECT COUNT(*) INTO constraint_exists
        FROM information_schema.table_constraints
        WHERE constraint_schema = 'zstack'
        AND table_name = table_name
        AND constraint_name = constraint_name
        AND constraint_type = 'FOREIGN KEY';

        -- Create constraint only if it doesn't exist
        IF constraint_exists = 0 THEN
            -- Build the ALTER TABLE statement dynamically
            SET @fk_sql = CONCAT(
                'ALTER TABLE `zstack`.`', table_name, '`',
                ' ADD CONSTRAINT `', constraint_name, '`',
                ' FOREIGN KEY (`', column_name, '`)',
                ' REFERENCES `zstack`.`', ref_table, '`(`', ref_column, '`)',
                ' ON DELETE ', on_delete_action
            );

            -- Prepare and execute the statement safely
            PREPARE stmt FROM @fk_sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END IF;
END $$
DELIMITER ;