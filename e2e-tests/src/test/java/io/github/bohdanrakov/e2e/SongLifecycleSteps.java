package io.github.bohdanrakov.e2e;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class SongLifecycleSteps {

    private static final String BASE_URL = System.getProperty("e2e.baseUrl", "http://localhost:8085");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private HttpResponse<String> lastResponse;
    private long resourceId;

    @When("I upload {string} through the API gateway")
    public void iUploadThroughTheApiGateway(String fileName) {
        lastResponse = send(HttpRequest.newBuilder(URI.create(BASE_URL + "/resources"))
                .header("Content-Type", "audio/mpeg")
                .POST(HttpRequest.BodyPublishers.ofByteArray(readResource(fileName))));
    }

    @Then("I receive a resource id")
    public void iReceiveAResourceId() {
        assertThat(lastResponse.statusCode()).isEqualTo(200);
        resourceId = JsonPath.<Number>read(lastResponse.body(), "$.id").longValue();
        assertThat(resourceId).isPositive();
    }

    @Then("within {int} seconds song metadata for that resource is available")
    public void songMetadataIsAvailableWithin(int seconds) {
        await().atMost(Duration.ofSeconds(seconds))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    lastResponse = get("/songs/" + resourceId);
                    assertThat(lastResponse.statusCode()).isEqualTo(200);
                });
    }

    @Then("the song metadata has a non-empty name and duration")
    public void theSongMetadataHasNameAndDuration() {
        assertThat(JsonPath.<Number>read(lastResponse.body(), "$.id").longValue()).isEqualTo(resourceId);
        assertThat(JsonPath.<String>read(lastResponse.body(), "$.name")).isNotBlank();
        assertThat(JsonPath.<String>read(lastResponse.body(), "$.duration")).isNotBlank();
    }

    @When("I delete the resource through the API gateway")
    public void iDeleteTheResourceThroughTheApiGateway() {
        lastResponse = send(HttpRequest.newBuilder(URI.create(BASE_URL + "/resources?id=" + resourceId)).DELETE());
        assertThat(lastResponse.statusCode()).isEqualTo(200);
        List<Number> deletedIds = JsonPath.read(lastResponse.body(), "$.ids");
        assertThat(deletedIds).map(Number::longValue).containsExactly(resourceId);
    }

    @Then("the resource is no longer available")
    public void theResourceIsNoLongerAvailable() {
        assertThat(get("/resources/" + resourceId).statusCode()).isEqualTo(404);
    }

    @Then("the song metadata is no longer available")
    public void theSongMetadataIsNoLongerAvailable() {
        assertThat(get("/songs/" + resourceId).statusCode()).isEqualTo(404);
    }

    private HttpResponse<String> get(String path) {
        return send(HttpRequest.newBuilder(URI.create(BASE_URL + path)).GET());
    }

    private HttpResponse<String> send(HttpRequest.Builder request) {
        try {
            return httpClient.send(request.timeout(Duration.ofSeconds(30)).build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IllegalStateException("Request to " + BASE_URL + " failed. Is the system running?", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static byte[] readResource(String fileName) {
        try (InputStream in = SongLifecycleSteps.class.getResourceAsStream("/" + fileName)) {
            return Objects.requireNonNull(in, fileName + " not found on classpath").readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
