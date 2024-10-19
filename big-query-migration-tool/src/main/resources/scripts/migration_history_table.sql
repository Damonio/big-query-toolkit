BEGIN
    CREATE TABLE IF NOT EXISTS `${default_dataset}.migration_history` (
        install_rank INT64 NOT NULL,
        file_name STRING NOT NULL,
        successful STRING NOT NULL,
        checksum STRING NOT NULL
    );
END;