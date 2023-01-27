package io.apicurio.registry.canary;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.apicurio.registry.canary.util.CanaryUtil;
import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.v2.beans.ArtifactMetaData;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.Path;

import static io.apicurio.registry.canary.util.CanaryUtil.createBasicClient;
import static io.apicurio.registry.canary.util.CanaryUtil.createOauthClient;

/**
 * Simple application performing the most basic operations of Apicurio Registry.
 * <p>
 * 1) Register a new schema in the Registry.
 * 2) Fetch the newly created schema.
 * 3) Delete the schema.
 *
 * @author Carles Arnal <carnalca@redhat.com>
 */
@Path("/")
public class CanaryMonitoring {

    private RegistryClient oAuthclient;
    private RegistryClient basicClient;
    private long failedReads = 0;
    private long failedCreates = 0;
    private long failedDeletes = 0;

    private static final Logger log = LoggerFactory.getLogger(CanaryMonitoring.class);

    @Gauge(name = "failedReads", unit = MetricUnits.NONE, description = "Failed read operations in the Canary application.")
    public Long failedReads() {
        return failedReads;
    }

    @Gauge(name = "failedCreates", unit = MetricUnits.NONE, description = "Failed create operations in the Canary application.")
    public Long failedCreates() {
        return failedCreates;
    }

    @Gauge(name = "failedDeletes", unit = MetricUnits.NONE, description = "Failed delete operations in the Canary application.")
    public Long failedDeletes() {
        return failedDeletes;
    }

    public void startMonitoring(@Observes StartupEvent startupEvent) {

        oAuthclient = createOauthClient();
        basicClient = createBasicClient();

        int concurrentTasks = 2;

        try {
            concurrentTasks = Integer.parseInt(System.getenv("CONCURRENT_TASKS"));
        } catch (Exception e) {
            log.warn("Cannot load concurrent tasks environment variable", e);
        }

        ExecutorService e = Executors.newFixedThreadPool(concurrentTasks);

        for (int i = 0; i < concurrentTasks; i++) {
            e.submit(() -> {
                log.info("Starting ouath-based apicurio registry monitoring");
                monitorApicurioRegistry(oAuthclient);
            });
            e.submit(() -> {
                log.info("Starting basic-auth-based apicurio registry monitoring");
                monitorApicurioRegistry(basicClient);
            });
        }
    }

    private void monitorApicurioRegistry(RegistryClient client) {
        while (true) {
            try {
                // Register the JSON Schema schema in Apicurio registry.
                final String artifactId = UUID.randomUUID().toString();
                createArtifact(client, artifactId);
                //Wait for the artifact to be available.
                Thread.sleep(500);
                readArtifact(client, artifactId);
                deleteArtifact(client, artifactId);
            } catch (Exception e) {
                log.error("Exception detected in the canary application: {}", e.getCause(), e);
            }
        }
    }

    private void createArtifact(RegistryClient client, String artifactId) {
        try {
            CanaryUtil.createSchemaInServiceRegistry(client, artifactId, Constants.SCHEMA);
        } catch (Exception e) {
            failedCreates++;
            log.error("Exception detected in the canary application: {}", e.getCause(), e);
        }
    }

    private void readArtifact(RegistryClient client, String artifactId) {
        try {
            final ArtifactMetaData schemaFromRegistry = CanaryUtil.getSchemaFromRegistry(client,
                    artifactId);
        } catch (Exception e) {
            failedReads++;
            log.error("Exception detected while reading an artifact in the canary application: {}", e.getCause(), e);
        }
    }

    private void deleteArtifact(RegistryClient client, String artifactId) {
        try {
            CanaryUtil.deleteSchema(client, artifactId);
        } catch (Exception e) {
            failedDeletes++;
            log.error("Exception detected while reading an artifact in the canary application: {}", e.getCause(), e);
        }
    }
}