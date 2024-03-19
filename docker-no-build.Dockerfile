# https://ktor.io/docs/docker.html
# https://ktor.io/docs/docker-compose.html

# No-build Dockerfile.
# Expected to be used with a pre-built jar file.

#-------------------------------------------------------------------------------------------------
# Final image stage.
FROM amazoncorretto:17
LABEL authors="perraco"
LABEL image.tag="kcrud"
LABEL name="kcrud-final-image"

# Expose the ports the container listens on during runtime.
EXPOSE 8080
EXPOSE 8443

# Create the final output directory.
RUN mkdir -p /app

# Copy the pre-built jar file from the source directory to the image.
COPY build/libs/kcrud-1.0.0-all.jar /app/kcrud-1.0.0-all.jar
# Copy the keystore file from the source directory to the image.
COPY keystore.p12 /app/keystore.p12

#-------------------------------------------------------------------------------------------------
# Environment variables.

# Set host to 0.0.0.0 to listens on all interfaces.
ENV KCRUD_KTOR_DEPLOYMENT_HOST="0.0.0.0"

# Set the SSL key location.
ENV KCRUD_KTOR_SECURITY_SSL_KEY_STORE="/app/keystore.p12"

# To override more configuration settings at image level add them here.
# For more settings see the existing 'conf' files in the base project, under the resources folder.
# Alternatively, the projct '.env' file can be used to override settings at runtime.

#-------------------------------------------------------------------------------------------------
# Execution entrypoint.
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/kcrud-1.0.0-all.jar"]