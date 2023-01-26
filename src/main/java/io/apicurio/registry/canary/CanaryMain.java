package io.apicurio.registry.canary;

import java.util.Collections;
import java.util.UUID;

import io.apicurio.registry.canary.util.RegistryCanaryUtil;
import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.RegistryClientFactory;
import io.apicurio.registry.rest.v2.beans.ArtifactMetaData;
import io.apicurio.rest.client.auth.BasicAuth;
import io.apicurio.rest.client.auth.OidcAuth;
import io.apicurio.rest.client.auth.exception.AuthErrorHandler;
import io.apicurio.rest.client.spi.ApicurioHttpClient;
import io.apicurio.rest.client.spi.ApicurioHttpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple application performing the most basic operations of Apicurio Registry.
 * <p>
 * 1) Register a new schema in the Registry.
 * 2) Fetch the newly created schema.
 * 3) Delete the schema.
 *
 * @author Carles Arnal <carnalca@redhat.com>
 */
public class CanaryMain {

	private static final RegistryClient oAuthclient;
	private static final RegistryClient basicClient;

	private static final Logger log = LoggerFactory.getLogger(CanaryMain.class);

	static {
		oAuthclient = createOauthClient();
		basicClient = createBasicClient();
	}

	public static void main(String[] args) throws Exception {
		try {
			while (true) {
                log.info("Starting ouath-based apicurio registry monitoring");
                monitorApicurioRegistry(oAuthclient);
                log.info("Starting basic auth-based apicurio registry monitoring");
                monitorApicurioRegistry(basicClient);
			}
		} catch (Exception e) {
			log.error("Exception detected in the canary application: {}", e.getCause(), e);
		}
	}

	private static void monitorApicurioRegistry(RegistryClient client) throws InterruptedException {
		// Register the JSON Schema schema in Apicurio registry.
		final String artifactId = UUID.randomUUID().toString();
		RegistryCanaryUtil.createSchemaInServiceRegistry(client, artifactId, Constants.SCHEMA);

		//Wait for the artifact to be available.
		Thread.sleep(1000);
		final ArtifactMetaData schemaFromRegistry = RegistryCanaryUtil.getSchemaFromRegistry(client,
				artifactId);
		RegistryCanaryUtil.deleteSchema(client, artifactId);

		//Wait 500ms for the next canary execution.
		Thread.sleep(500);

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