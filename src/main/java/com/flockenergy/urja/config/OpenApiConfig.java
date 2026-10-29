package com.flockenergy.urja.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI urjaMeterOpsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Urja Meter Ops REST API")
                        .version("1.0.0")
                        .description("""
                                Clean, developer-friendly, documented REST API service built on top of the legacy Urja Meter Ops portal.
                                
                                Features:
                                - **Meters API**: Canonical meter data, multi-attribute filtering (make, phase, status, DT), and geospatial radius queries.
                                - **Consumption & Readings**: Normalized interval energy readings with active/apparent energy and voltage quality analytics.
                                - **Distribution Transformers**: Transformer capacity tracking and connected meter aggregation.
                                - **Network Hierarchy**: Complete reconstruction of the distribution network from Zone to DT.
                                - **Dataset Sync**: Proactive in-memory indexing via HMAC-SHA256 authenticated bulk export.
                                """)
                        .contact(new Contact()
                                .name("Flock Energy Engineering")
                                .url("https://flockenergy.tech"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("/").description("Current Server (Local or Deployed)")
                ));
    }
}
