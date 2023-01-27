package io.apicurio.registry.canary.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import io.apicurio.registry.rest.client.RegistryClientFactory;
import io.apicurio.rest.client.auth.BasicAuth;
import io.apicurio.rest.client.auth.OidcAuth;
import io.apicurio.rest.client.auth.exception.AuthErrorHandler;
import io.apicurio.rest.client.spi.ApicurioHttpClient;
import io.apicurio.rest.client.spi.ApicurioHttpClientFactory;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.v2.beans.ArtifactMetaData;
import io.apicurio.registry.rest.v2.beans.IfExists;
import io.apicurio.registry.types.ArtifactType;

public class CanaryUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CanaryUtil.class);

    /**
     * Create the artifact in the registry (or update it if it already exists).
     *
     * @param artifactId
     * @param schema
     */
    @Counted(name = "artifactsCreated", description = "How many artifacts the canary application has created.")
    public static void createSchemaInServiceRegistry(RegistryClient service, String artifactId, String schema) {

        LOGGER.info("---------------------------------------------------------");
        LOGGER.info("=====> Creating artifact in the registry for JSON Schema with ID: {}", artifactId);
        final ByteArrayInputStream content = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
        final ArtifactMetaData metaData = service.createArtifact("default", artifactId, ArtifactType.JSON, IfExists.RETURN, content);
        assert metaData != null;
        LOGGER.info("=====> Successfully created JSON Schema artifact in Service Registry: {}", metaData);
        LOGGER.info("---------------------------------------------------------");
    }

    /**
     * Get the artifact from the registry.
     *
     * @param artifactId
     */
    @Counted(name = "artifactsDownloaded", description = "How many artifacts the canary application has downloaded.")
    public static ArtifactMetaData getSchemaFromRegistry(RegistryClient service, String artifactId) {

        LOGGER.info("---------------------------------------------------------");
        LOGGER.info("=====> Fetching artifact from the registry for JSON Schema with ID: {}", artifactId);
        final ArtifactMetaData metaData = service.getArtifactMetaData("default", artifactId);
        assert metaData != null;
        LOGGER.info("=====> Successfully fetched JSON Schema artifact metadata in Service Registry: {}", metaData);
        LOGGER.info("---------------------------------------------------------");

        LOGGER.info("---------------------------------------------------------");
        LOGGER.info("=====> Fetching artifact content from the registry for JSON Schema with ID: {}", artifactId);
        final InputStream content = service.getLatestArtifact("default", artifactId);
        assert content != null;
        LOGGER.info("=====> Successfully fetched JSON Schema content in Service Registry: {}", metaData);
        LOGGER.info("---------------------------------------------------------");

        return metaData;
    }

    /**
     * Delete the artifact from the registry.
     *
     * @param artifactId
     */
    public static void deleteSchema(RegistryClient service, String artifactId) {

        LOGGER.info("---------------------------------------------------------");
        LOGGER.info("=====> Deleting artifact from the registry for JSON Schema with ID: {}", artifactId);
        service.deleteArtifact("default", artifactId);
        LOGGER.info("=====> Successfully deleted JSON Schema artifact in Service Registry.");
        LOGGER.info("---------------------------------------------------------");
    }

    public static RegistryClient createOauthClient() {
        final String registryUrl = System.getenv("REGISTRY_URL");
        final String tokenEndpoint = System.getenv("AUTH_TOKEN_ENDPOINT");
        if (tokenEndpoint != null) {
            final String authClient = System.getenv("AUTH_CLIENT_ID");
            final String authSecret = System.getenv("AUTH_CLIENT_SECRET");
            final ApicurioHttpClient httpClient = ApicurioHttpClientFactory.create(tokenEndpoint,
                    Collections.emptyMap(), null, new AuthErrorHandler());
            return RegistryClientFactory.create(registryUrl, Collections.emptyMap(),
                    new OidcAuth(httpClient, authClient, authSecret));
        } else {
            return RegistryClientFactory.create(registryUrl);
        }
    }

    public static RegistryClient createBasicClient() {
        final String registryUrl = System.getenv("REGISTRY_URL");
        final String authClient = System.getenv("AUTH_CLIENT_ID");
        final String authSecret = System.getenv("AUTH_CLIENT_SECRET");
        return RegistryClientFactory.create(registryUrl, Collections.emptyMap(),
                new BasicAuth(authClient, authSecret));
    }
}
