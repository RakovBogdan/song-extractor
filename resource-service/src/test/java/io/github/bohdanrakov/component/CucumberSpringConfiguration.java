package io.github.bohdanrakov.component;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.client.enabled=false")
public class CucumberSpringConfiguration {

    static final String BUCKET_NAME = "staging-mp3-storage";
    static final String QUEUE_NAME = "exchange-resource-id.resource-processor";

    @ServiceConnection
    static final PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer("postgres:17-alpine")
            .withCopyFileToContainer(MountableFile.forHostPath("../init-scripts/resource-db/init.sql"),
                    "/docker-entrypoint-initdb.d/init.sql");

    @ServiceConnection
    static final RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:4-management-alpine");

    static final LocalStackContainer localstackContainer =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:4")).withServices("s3");

    static final WireMockServer songServiceStub = new WireMockServer(wireMockConfig().dynamicPort());

    static final WireMockServer storageServiceStub = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        Startables.deepStart(postgresqlContainer, rabbitMQContainer, localstackContainer).join();
        songServiceStub.start();
        storageServiceStub.start();
        storageServiceStub.stubFor(get(urlPathEqualTo("/storages")).willReturn(okJson("""
                [
                  {"id": 1, "storageType": "STAGING", "bucket": "%s", "path": "/files"},
                  {"id": 2, "storageType": "PERMANENT", "bucket": "permanent-mp3-storage", "path": "/files"}
                ]
                """.formatted(BUCKET_NAME))));
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("aws.s3.url", () -> localstackContainer.getEndpoint().toString());
        registry.add("aws.s3.region", localstackContainer::getRegion);
        registry.add("aws.accessKeyId", localstackContainer::getAccessKey);
        registry.add("aws.secretAccessKey", localstackContainer::getSecretKey);

        registry.add("spring.cloud.discovery.client.simple.instances.song-service[0].uri",
                songServiceStub::baseUrl);
        registry.add("spring.cloud.discovery.client.simple.instances.storage-service[0].uri",
                storageServiceStub::baseUrl);
    }
}
