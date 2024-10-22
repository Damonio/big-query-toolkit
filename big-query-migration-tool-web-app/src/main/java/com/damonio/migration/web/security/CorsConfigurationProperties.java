package com.damonio.migration.web.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("cors")
public class CorsConfigurationProperties {
    private Boolean disableCors;
}
