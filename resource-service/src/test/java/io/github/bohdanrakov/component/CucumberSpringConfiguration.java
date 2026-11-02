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

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.client.enabled=false")
public class CucumberSpringConfiguration {

    static final String BUCKET_NAME = "mp3-storage";
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

    static {
        Startables.deepStart(postgresqlContainer, rabbitMQContainer, localstackContainer).join();
        songServiceStub.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("aws.s3.url", () -> localstackContainer.getEndpoint().toString());
        registry.add("aws.s3.region", localstackContainer::getRegion);
        registry.add("aws.accessKeyId", localstackContainer::getAccessKey);
        registry.add("aws.secretAccessKey", localstackContainer::getSecretKey);
        registry.add("aws.s3.bucket-name", () -> BUCKET_NAME);

        registry.add("spring.cloud.discovery.client.simple.instances.song-service[0].uri",
                songServiceStub::baseUrl);
    }
}
