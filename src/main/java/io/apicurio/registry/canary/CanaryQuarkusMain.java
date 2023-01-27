package io.apicurio.registry.canary;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain(name = "CanaryQuarkusMain")
public class CanaryQuarkusMain {
    public static void main(String... args) {
        Quarkus.run(args);
    }
}
