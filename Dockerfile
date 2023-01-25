FROM openjdk:17
# copy the packaged jar file into our docker image
COPY target/apicurio-registry-multitenant-canary-application-*.jar /canary.jar
# set the startup command to execute the jar
CMD ["java", "-jar", "/canary.jar"]