package io.apicurio.registry.probe;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain(name = "ProbeQuarkusMain")
public class ProbeQuarkusMain {
    public static void main(String... args) {
        Quarkus.run(args);
    }
}
