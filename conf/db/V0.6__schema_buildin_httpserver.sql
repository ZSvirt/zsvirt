CREATE TABLE  `zstack_rest`.`RestAPIVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `apiMessageName` varchar(255) DEFAULT NULL,
    `state` varchar(255) NOT NULL,
    `result` text DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
