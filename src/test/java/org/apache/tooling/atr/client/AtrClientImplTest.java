/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tooling.atr.client;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WireMockTest
class AtrClientImplTest {

    private AtrClient atrClient;

    @BeforeEach
    void setup(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        atrClient =
                new AtrClientImpl(new URL(wmRuntimeInfo.getHttpBaseUrl()), "user", "password", new AtomicReference<>());
    }

    @Test
    void getReleaseShouldBeReturned() throws Exception {
        stubForJwtCreate();

        stubFor(get(urlEqualTo("/api/release/get/test-project/1.0.0"))
                .withHeader("Authorization", equalTo("Bearer test-jwt"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"release\":{"
                                + "  \"version\":\"1.0.0\","
                                + "  \"phase\":\"release_candidate_draft\""
                                + "}}")));

        AtrClient.ReleaseInfo release = atrClient.getRelease("test-project", "1.0.0");

        assertNotNull(release);
        assertEquals("1.0.0", release.getVersion());
        assertTrue(release.isComposing());

        // second call should use the cached JWT
        release = atrClient.getRelease("test-project", "1.0.0");
        assertNotNull(release);

        // verify that the JWT was only created once and the release was requested twice
        verify(2, getRequestedFor(urlEqualTo("/api/release/get/test-project/1.0.0")));
        verify(
                1,
                postRequestedFor(urlEqualTo("/api/jwt/create"))
                        .withRequestBody(equalTo("{\"asfuid\":\"user\",\"pat\":\"password\"}")));
    }

    @Test
    void unfoundReleaseShouldReturnNull() throws Exception {
        stubForJwtCreate();

        stubFor(get(urlEqualTo("/api/release/get/test-project/1.0.0"))
                .withHeader("Authorization", equalTo("Bearer test-jwt"))
                .willReturn(notFound()));

        AtrClient.ReleaseInfo release = atrClient.getRelease("test-project", "1.0.0");

        assertNull(release);
    }

    @Test
    void serverErrorShouldThrowException() {
        stubForJwtCreate();

        stubFor(get(urlEqualTo("/api/release/get/test-project/1.0.0"))
                .withHeader("Authorization", equalTo("Bearer test-jwt"))
                .willReturn(aResponse().withStatus(500).withBody("{\"error\": \"Internal server error\"}")));

        AtrClientException exception =
                assertThrows(AtrClientException.class, () -> atrClient.getRelease("test-project", "1.0.0"));

        assertTrue(exception.getMessage().contains("\"Internal server error\""));
    }

    @Test
    void createReleaseShouldSendCorrectPayload() throws Exception {
        stubForJwtCreate();

        stubFor(post(urlEqualTo("/api/release/create"))
                .withHeader("Authorization", equalTo("Bearer test-jwt"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"release\":{"
                                + "  \"version\":\"2.0.0\","
                                + "  \"phase\":\"release_candidate_draft\""
                                + "}}")));

        AtrClient.ReleaseInfo release = atrClient.createRelease("my-project", "2.0.0");

        assertNotNull(release);
        assertEquals("2.0.0", release.getVersion());

        // Verify the request body contains correct project and version
        verify(
                1,
                postRequestedFor(urlEqualTo("/api/release/create"))
                        .withHeader("Authorization", equalTo("Bearer test-jwt"))
                        .withRequestBody(matchingJsonPath("$.project", equalTo("my-project")))
                        .withRequestBody(matchingJsonPath("$.version", equalTo("2.0.0"))));
    }

    @Test
    void createReleaseShouldUseCachedJwt() throws Exception {
        stubForJwtCreate();

        stubFor(post(urlEqualTo("/api/release/create"))
                .withHeader("Authorization", equalTo("Bearer test-jwt"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"release\":{"
                                + "  \"version\":\"1.0.0\","
                                + "  \"phase\":\"release_candidate_draft\""
                                + "}}")));

        // First call
        atrClient.createRelease("test-project", "1.0.0");

        // Second call should reuse cached JWT
        atrClient.createRelease("test-project", "1.0.0");

        // Verify that JWT was only created once and release create was called twice
        verify(1, postRequestedFor(urlEqualTo("/api/jwt/create")));
        verify(2, postRequestedFor(urlEqualTo("/api/release/create")));
    }

    @Test
    void createReleaseServerErrorShouldThrowException() {
        stubForJwtCreate();

        stubFor(post(urlEqualTo("/api/release/create"))
                .withHeader("Authorization", equalTo("Bearer test-jwt"))
                .willReturn(aResponse().withStatus(500).withBody("{\"error\": \"Internal server error\"}")));

        AtrClientException exception =
                assertThrows(AtrClientException.class, () -> atrClient.createRelease("test-project", "1.0.0"));

        assertTrue(exception.getMessage().contains("\"Internal server error\""));
    }

    @Test
    void uploadFileShouldSucceedWithoutRevisionNumber(@TempDir Path tempDir) throws Exception {
        stubForJwtCreate();

        // Create test file
        Path testFile = tempDir.resolve("test-file.txt");
        Files.write(testFile, "Test content".getBytes());

        stubFor(post(urlEqualTo("/api/release/upload"))
                .withHeader("Authorization", equalTo("Bearer test-jwt"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"endpoint\":\"test-endpoint\"}")));

        String revision = atrClient.uploadFile("test-project", "1.0.0", "test-file.txt", testFile);

        assertEquals("unknown", revision);
    }

    @Test
    void uploadFileShouldSendCorrectPayloadInPostRequest(@TempDir Path tempDir) throws Exception {
        stubForJwtCreate();

        // Create test file with specific content
        Path testFile = tempDir.resolve("test-artifact.zip");
        String testContent = "Binary file content";
        Files.write(testFile, testContent.getBytes());
        String expectedBase64 = Base64.getEncoder().encodeToString(testContent.getBytes());

        stubFor(post(urlEqualTo("/api/release/upload"))
                .withHeader("Authorization", equalTo("Bearer test-jwt"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"endpoint\":\"test-endpoint\",\"revision\":{" + "  \"number\":\"42\"}}")));

        String revision = atrClient.uploadFile("my-project", "2.0.0", "release/test-artifact.zip", testFile);

        // Verify the request body contains all required JSON fields with correct values
        verify(
                1,
                postRequestedFor(urlEqualTo("/api/release/upload"))
                        .withHeader("Authorization", equalTo("Bearer test-jwt"))
                        .withRequestBody(matchingJsonPath("$.project", equalTo("my-project")))
                        .withRequestBody(matchingJsonPath("$.version", equalTo("2.0.0")))
                        .withRequestBody(matchingJsonPath("$.relpath", equalTo("release/test-artifact.zip")))
                        .withRequestBody(matchingJsonPath("$.content", equalTo(expectedBase64))));

        assertEquals("42", revision);
    }

    @Test
    void uploadFileShouldThrowExceptionWhenFileNotFound() {
        stubForJwtCreate();

        Path nonExistentFile = Paths.get("/non/existent/path/file.txt");

        AtrClientException exception = assertThrows(
                AtrClientException.class,
                () -> atrClient.uploadFile("test-project", "1.0.0", "test-file.txt", nonExistentFile));
        assertInstanceOf(NoSuchFileException.class, exception.getCause());
    }

    @Test
    void uploadFileServerErrorShouldThrowException(@TempDir Path tempDir) throws Exception {
        stubForJwtCreate();

        // Create test file
        Path testFile = tempDir.resolve("test-file.txt");
        Files.write(testFile, "Test content".getBytes());

        stubFor(post(urlEqualTo("/api/release/upload"))
                .withHeader("Authorization", equalTo("Bearer test-jwt"))
                .willReturn(aResponse().withStatus(500).withBody("{\"error\": \"Server error\"}")));

        AtrClientException exception = assertThrows(
                AtrClientException.class,
                () -> atrClient.uploadFile("test-project", "1.0.0", "test-file.txt", testFile));

        assertTrue(exception.getMessage().contains("\"Server error\""));
    }

    @Test
    void uploadFileShouldUseCachedJwt(@TempDir Path tempDir) throws Exception {
        stubForJwtCreate();

        // Create test file
        Path testFile = tempDir.resolve("test-file.txt");
        Files.write(testFile, "Test content".getBytes());

        stubFor(post(urlEqualTo("/api/release/upload"))
                .withHeader("Authorization", equalTo("Bearer test-jwt"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"endpoint\":\"test-endpoint\",\"revision\":{" + "  \"number\":\"1\"}}")));

        // First upload
        atrClient.uploadFile("test-project", "1.0.0", "test-file.txt", testFile);

        // Second upload should reuse cached JWT
        atrClient.uploadFile("test-project", "1.0.0", "test-file.txt", testFile);

        // Verify that JWT was only created once and uploads were performed twice
        verify(1, postRequestedFor(urlEqualTo("/api/jwt/create")));
        verify(2, postRequestedFor(urlEqualTo("/api/release/upload")));
    }

    private void stubForJwtCreate() {
        stubFor(post(urlEqualTo("/api/jwt/create"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"jwt\":\"test-jwt\"}")));
    }
}
