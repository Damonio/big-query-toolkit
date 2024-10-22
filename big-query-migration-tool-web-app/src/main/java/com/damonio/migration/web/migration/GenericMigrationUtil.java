package com.damonio.migration.web.migration;

import com.damonio.migration.BigQueryMigrationConfiguration;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.io.FileSystemResource;

import java.io.File;

@UtilityClass
public class GenericMigrationUtil {

    @SneakyThrows
    public static BigQueryMigrationConfiguration readConfigurationFile(String environmentFileName, String extractedLocation, String bindProperty) {
        var file = new File(extractedLocation + File.separator + environmentFileName);
        return readYAMLASPojo(bindProperty, file);
    }

    private static BigQueryMigrationConfiguration readYAMLASPojo(String bindProperty, File file) {
        var yamlFactory = new YamlPropertiesFactoryBean();
        yamlFactory.setResources(new FileSystemResource(file));

        var properties = yamlFactory.getObject();

        var propertySource = new MapConfigurationPropertySource(properties);
        var binder = new Binder(propertySource);

        return binder.bind(bindProperty, BigQueryMigrationConfiguration.class).get();
    }
}