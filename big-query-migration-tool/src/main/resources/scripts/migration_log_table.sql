BEGIN
    CREATE TABLE IF NOT EXISTS `${default_dataset}.migration_log` (
        install_rank INT64 NOT NULL,
        file_name STRING NOT NULL,
        successful BOOL NOT NULL,
        checksum STRING NOT NULL
    );
END;