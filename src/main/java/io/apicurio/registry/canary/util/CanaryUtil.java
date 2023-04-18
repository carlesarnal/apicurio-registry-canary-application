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
import org.eclipse.microprofile.metrics.annotation.Timed;
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
    @Timed
    public static void createSchemaInServiceRegistry(RegistryClient service, String artifactId, String schema) {

             final ByteArrayInputStream content = new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8));
        final ArtifactMetaData metaData = service.createArtifact("default", artifactId, ArtifactType.JSON, IfExists.RETURN, content);
        assert metaData != null;
    }

    /**
     * Get the artifact from the registry.
     *
     * @param artifactId
     */
    public static ArtifactMetaData getSchemaFromRegistry(RegistryClient service, String artifactId) {

        final ArtifactMetaData metaData = getSchemaMetadata(service, artifactId);
        assert metaData != null;

        final InputStream content = getSchemaContentFromRegistry(service, artifactId);
        assert content != null;

        return metaData;
    }

    /**
     * Get the artifact from the registry.
     *
     * @param artifactId
     */
    @Counted(name = "artifactsContentDownloaded", description = "How many artifacts the canary application has downloaded.")
    @Timed
    public static InputStream getSchemaContentFromRegistry(RegistryClient service, String artifactId) {

        final InputStream content = service.getLatestArtifact("default", artifactId);
        assert content != null;

        return content;
    }

    /**
     * Get the artifact from the registry.
     *
     * @param artifactId
     */
    @Counted(name = "artifactMetadataFetched", description = "How many artifact metadata the canary application has downloaded.")
    @Timed
    public static ArtifactMetaData getSchemaMetadata(RegistryClient service, String artifactId) {

        final ArtifactMetaData metaData = service.getArtifactMetaData("default", artifactId);
        assert metaData != null;

        return metaData;
    }

    /**
     * Delete the artifact from the registry.
     *
     * @param artifactId
     */
    @Timed
    @Counted(name = "artifactsDeleted", description = "How many artifacts the canary application has deleted.")
    public static void deleteSchema(RegistryClient service, String artifactId) {

        service.deleteArtifact("default", artifactId);
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
