package io.github.bohdanrakov.component;

import com.jayway.jsonpath.JsonPath;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.bohdanrakov.repositories.MP3FileRepository;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.github.bohdanrakov.component.CucumberSpringConfiguration.BUCKET_NAME;
import static io.github.bohdanrakov.component.CucumberSpringConfiguration.QUEUE_NAME;
import static io.github.bohdanrakov.component.CucumberSpringConfiguration.songServiceStub;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class ResourceSteps {

    @LocalServerPort
    private int port;

    @Autowired
    private MP3FileRepository mp3FileRepository;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    private ResponseEntity<byte[]> lastResponse;
    private final List<Long> uploadedIds = new ArrayList<>();

    @Given("resource storage is empty")
    public void resourceStorageIsEmpty() {
        mp3FileRepository.deleteAll();
        getS3Keys().forEach(key -> s3Client.deleteObject(b -> b.bucket(BUCKET_NAME).key(key)));
        amqpAdmin.purgeQueue(QUEUE_NAME, false);
        songServiceStub.resetAll();
    }

    @Given("song-service accepts metadata deletion")
    public void songServiceAcceptsMetadataDeletion() {
        songServiceStub.stubFor(delete(urlPathEqualTo("/songs")).willReturn(okJson("{\"ids\":[]}")));
    }

    @Given("{string} has been uploaded {int} times")
    public void fileHasBeenUploadedTimes(String fileName, int times) {
        for (int i = 0; i < times; i++) {
            iUploadFileAs(fileName, "audio/mpeg");
            assertThat(lastResponse.getStatusCode().value()).isEqualTo(200);
            uploadedIds.add(readLong("$.id"));
        }
    }

    @When("I upload {string} as {string}")
    public void iUploadFileAs(String fileName, String contentType) {
        upload(readResource(fileName), contentType);
    }

    @When("I upload the text {string} as {string}")
    public void iUploadTextAs(String text, String contentType) {
        upload(text.getBytes(UTF_8), contentType);
    }

    @When("I request resource {string}")
    public void iRequestResource(String id) {
        lastResponse = client().get()
                .uri("/resources/{id}", id)
                .retrieve()
                .onStatus(status -> true, (request, response) -> { })
                .toEntity(byte[].class);
    }

    @When("I delete the uploaded resources together with id {long}")
    public void iDeleteUploadedResourcesTogetherWith(long unknownId) {
        String ids = uploadedIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + "," + unknownId;
        lastResponse = client().delete()
                .uri(uriBuilder -> uriBuilder.path("/resources").queryParam("id", ids).build())
                .retrieve()
                .onStatus(status -> true, (request, response) -> { })
                .toEntity(byte[].class);
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        assertThat(lastResponse.getStatusCode().value()).isEqualTo(status);
    }

    @Then("the response contains a resource id")
    public void theResponseContainsAResourceId() {
        uploadedIds.add(readLong("$.id"));
        assertThat(uploadedIds.getFirst()).isPositive();
    }

    @Then("the stored resource can be downloaded and is identical to {string}")
    public void theStoredResourceCanBeDownloaded(String fileName) {
        iRequestResource(uploadedIds.getFirst().toString());

        assertThat(lastResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(lastResponse.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("audio/mpeg"));
        assertThat(lastResponse.getBody()).isEqualTo(readResource(fileName));
    }

    @Then("a resource stored event with that id is published")
    public void aResourceStoredEventIsPublished() {
        Message event = rabbitTemplate.receive(QUEUE_NAME, 1000);
        assertThat(event).isNotNull();
        assertThat(new String(event.getBody(), UTF_8)).isEqualTo(uploadedIds.getFirst().toString());
    }

    @Then("nothing is stored and no event is published")
    public void nothingIsStoredAndNoEventIsPublished() {
        assertThat(mp3FileRepository.count()).isZero();
        assertThat(getS3Keys()).isEmpty();
        assertThat(rabbitTemplate.receive(QUEUE_NAME, 1000)).isNull();
    }

    @Then("the response lists only the uploaded ids")
    public void theResponseListsOnlyTheUploadedIds() {
        List<Number> deletedIds = JsonPath.read(responseBody(), "$.ids");
        assertThat(deletedIds).map(Number::longValue).containsExactlyInAnyOrderElementsOf(uploadedIds);
        assertThat(mp3FileRepository.count()).isZero();
        assertThat(getS3Keys()).isEmpty();
    }

    @Then("song-service was asked to delete metadata for the uploaded ids")
    public void songServiceWasAskedToDeleteMetadata() {
        List<LoggedRequest> requests = songServiceStub.findAll(deleteRequestedFor(urlPathEqualTo("/songs")));
        assertThat(requests).hasSize(1);
        String requestedIds = requests.getFirst().queryParameter("id").firstValue();
        assertThat(Arrays.stream(requestedIds.split(",")).map(Long::valueOf).toList())
                .containsExactlyInAnyOrderElementsOf(uploadedIds);
    }

    private void upload(byte[] content, String contentType) {
        lastResponse = client().post()
                .uri("/resources")
                .contentType(MediaType.parseMediaType(contentType))
                .body(content)
                .retrieve()
                .onStatus(status -> true, (request, response) -> { })
                .toEntity(byte[].class);
    }

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    private long readLong(String jsonPath) {
        return JsonPath.<Number>read(responseBody(), jsonPath).longValue();
    }

    private String responseBody() {
        return new String(Objects.requireNonNull(lastResponse.getBody()), UTF_8);
    }

    private static byte[] readResource(String fileName) {
        try (InputStream in = ResourceSteps.class.getResourceAsStream("/" + fileName)) {
            return Objects.requireNonNull(in).readAllBytes();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private List<String> getS3Keys() {
        return s3Client.listObjectsV2(b -> b.bucket(BUCKET_NAME)).contents().stream()
                .map(S3Object::key)
                .toList();
    }
}
