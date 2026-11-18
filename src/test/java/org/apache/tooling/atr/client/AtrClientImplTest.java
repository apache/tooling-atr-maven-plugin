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
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
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
        atrClient = new AtrClientImpl(new URL(wmRuntimeInfo.getHttpBaseUrl()), "user", "pat", new AtomicReference<>());
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
                        .withRequestBody(equalTo("{\"asfuid\":\"user\",\"pat\":\"pat\"}")));
    }

    @ParameterizedTest
    @CsvSource(
            value = {"user, null", "null, pat", "null, null"},
            nullValues = "null")
    void missingCredentialsShouldThrowException(String username, String pat, WireMockRuntimeInfo wmRuntimeInfo)
            throws Exception {
        AtrClient client =
                new AtrClientImpl(new URL(wmRuntimeInfo.getHttpBaseUrl()), username, pat, new AtomicReference<>());

        AtrClientException exception =
                assertThrows(AtrClientException.class, () -> client.getRelease("test-project", "1.0.0"));

        assertTrue(exception.getMessage().startsWith("Missing ATR credentials"));
        verify(0, postRequestedFor(urlEqualTo("/api/jwt/create")));
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
    void getProjectShouldBeReturned() throws Exception {
        stubForJwtCreate();

        stubFor(get(urlEqualTo("/api/project/get/test-project"))
                .withHeader("Authorization", equalTo("Bearer test-jwt"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"project\":{"
                                + "  \"key\":\"test-project\","
                                + "  \"name\":\"Test Project\""
                                + "}}")));

        AtrClient.ProjectInfo project = atrClient.getProject("test-project");

        assertNotNull(project);
        assertEquals("test-project", project.getKey());
        assertEquals("Test Project", project.getName());
    }

    @Test
    void unfoundProjectShouldReturnNull() throws Exception {
        stubForJwtCreate();

        stubFor(get(urlEqualTo("/api/project/get/test-project"))
                .withHeader("Authorization", equalTo("Bearer test-jwt"))
                .willReturn(notFound()));

        AtrClient.ProjectInfo project = atrClient.getProject("test-project");

        assertNull(project);
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
    void setSourceCommitShouldSendCorrectPayload() throws Exception {
        stubForJwtCreate();

        stubFor(post(urlEqualTo("/api/release/commit"))
                .withHeader("Authorization", equalTo("Bearer test-jwt"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"endpoint\":\"/release/commit\",\"release\":{"
                                + "  \"version\":\"2.0.0\","
                                + "  \"phase\":\"release_candidate_draft\""
                                + "}}")));

        AtrClient.ReleaseInfo release =
                atrClient.setSourceCommit("my-project", "2.0.0", "0123456789abcdef0123456789abcdef01234567");

        assertNotNull(release);
        assertEquals("2.0.0", release.getVersion());

        verify(
                1,
                postRequestedFor(urlEqualTo("/api/release/commit"))
                        .withHeader("Authorization", equalTo("Bearer test-jwt"))
                        .withHeader("Content-Type", equalTo("application/json"))
                        .withRequestBody(matchingJsonPath("$.project", equalTo("my-project")))
                        .withRequestBody(matchingJsonPath("$.version", equalTo("2.0.0")))
                        .withRequestBody(
                                matchingJsonPath("$.commit", equalTo("0123456789abcdef0123456789abcdef01234567"))));
    }

    @Test
    void setSourceCommitServerErrorShouldThrowException() {
        stubForJwtCreate();

        stubFor(post(urlEqualTo("/api/release/commit"))
                .withHeader("Authorization", equalTo("Bearer test-jwt"))
                .willReturn(aResponse().withStatus(400).withBody("{\"error\": \"Invalid commit\"}")));

        AtrClientException exception = assertThrows(
                AtrClientException.class,
                () -> atrClient.setSourceCommit("test-project", "1.0.0", "0123456789abcdef0123456789abcdef01234567"));

        assertTrue(exception.getMessage().contains("\"Invalid commit\""));
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

        stubFor(post(urlPathEqualTo("/api/release/store"))
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

        stubFor(post(urlPathEqualTo("/api/release/store"))
                .withHeader("Authorization", equalTo("Bearer test-jwt"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"endpoint\":\"/release/store\",\"quarantined\":false,"
                                + "\"revision\":{\"number\":\"42\"}}")));

        String revision = atrClient.uploadFile("my-project", "2.0.0", "release/test-artifact.zip", testFile);

        // Verify the query parameters and the raw file content in the request body
        verify(
                1,
                postRequestedFor(urlPathEqualTo("/api/release/store"))
                        .withHeader("Authorization", equalTo("Bearer test-jwt"))
                        .withHeader("Content-Type", equalTo("application/octet-stream"))
                        .withQueryParam("project", equalTo("my-project"))
                        .withQueryParam("version", equalTo("2.0.0"))
                        .withQueryParam("relpath", equalTo("release/test-artifact.zip"))
                        .withRequestBody(equalTo(testContent)));

        assertEquals("42", revision);
    }

    @Test
    void uploadFileShouldReturnQuarantined(@TempDir Path tempDir) throws Exception {
        stubForJwtCreate();

        Path testFile = tempDir.resolve("test-artifact.zip");
        Files.write(testFile, "Archive content".getBytes());

        stubFor(post(urlPathEqualTo("/api/release/store"))
                .withHeader("Authorization", equalTo("Bearer test-jwt"))
                .willReturn(aResponse()
                        .withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"endpoint\":\"/release/store\",\"quarantined\":true,\"revision\":null}")));

        String revision = atrClient.uploadFile("my-project", "2.0.0", "test-artifact.zip", testFile);

        assertEquals("quarantined", revision);
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

        stubFor(post(urlPathEqualTo("/api/release/store"))
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

        stubFor(post(urlPathEqualTo("/api/release/store"))
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
        verify(2, postRequestedFor(urlPathEqualTo("/api/release/store")));
    }

    @Test
    void patJwtShouldExpireAfter30Minutes(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        stubForJwtCreate();
        stubForReleaseCreate();
        AtomicReference<Jwt> jwtCache = new AtomicReference<>();
        AtrClient client = new AtrClientImpl(new URL(wmRuntimeInfo.getHttpBaseUrl()), "user", "pat", jwtCache);

        long before = Instant.now().getEpochSecond();
        client.createRelease("test-project", "1.0.0");
        long after = Instant.now().getEpochSecond();

        assertEquals("test-jwt", jwtCache.get().getToken());
        assertTrue(jwtCache.get().getExpires() >= before + 30 * 60);
        assertTrue(jwtCache.get().getExpires() <= after + 30 * 60);
    }

    @Test
    void expiredPatJwtShouldBeCreatedAgain(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        stubForJwtCreate();
        stubForReleaseCreate();
        // JWT cached by a previous goal, about to expire
        AtomicReference<Jwt> jwtCache =
                new AtomicReference<>(new Jwt("old-jwt", Instant.now().getEpochSecond() + 10));
        AtrClient client = new AtrClientImpl(new URL(wmRuntimeInfo.getHttpBaseUrl()), "user", "pat", jwtCache);

        client.createRelease("test-project", "1.0.0");

        verify(1, postRequestedFor(urlEqualTo("/api/jwt/create")).withoutHeader("Authorization"));
        verify(
                1,
                postRequestedFor(urlEqualTo("/api/release/create"))
                        .withHeader("Authorization", equalTo("Bearer test-jwt")));
        assertEquals("test-jwt", jwtCache.get().getToken());
    }

    @Test
    void trustedPublisherUploadShouldExchangeGitHubOidcToken(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo)
            throws Exception {
        AtrClient client = createTrustedPublisherClient(wmRuntimeInfo, "0123456789abcdef0123456789abcdef01234567");
        stubForGitHubOidcToken();
        stubForPublisherJwtCreate(Instant.now().plusSeconds(300).getEpochSecond());
        stubForStoreWithProjectJwt();

        Path testFile = tempDir.resolve("test-file.txt");
        Files.write(testFile, "Test content".getBytes());

        assertTrue(client.isGitHubOidc());
        client.uploadFile("test-project", "1.0.0", "test-file.txt", testFile);
        client.uploadFile("test-project", "1.0.0", "test-file.txt", testFile);

        // the audience is the ATR server
        verify(
                1,
                getRequestedFor(urlPathEqualTo("/github/oidc"))
                        .withHeader("Authorization", equalTo("Bearer gh-request-token"))
                        .withQueryParam("api-version", equalTo("2.0"))
                        .withQueryParam("audience", equalTo(wmRuntimeInfo.getHttpBaseUrl() + "/")));
        // the source commit is given at the exchange, not with the release/commit endpoint
        verify(
                1,
                postRequestedFor(urlEqualTo("/api/publisher/jwt/create"))
                        .withoutHeader("Authorization")
                        .withRequestBody(matchingJsonPath("$.publisher", equalTo("github")))
                        .withRequestBody(matchingJsonPath("$.jwt", equalTo("gh-oidc-token")))
                        .withRequestBody(matchingJsonPath(
                                "$.source_commit", equalTo("0123456789abcdef0123456789abcdef01234567"))));
        verify(0, postRequestedFor(urlEqualTo("/api/release/commit")));
        verify(0, postRequestedFor(urlEqualTo("/api/jwt/create")));
        verify(2, postRequestedFor(urlPathEqualTo("/api/release/store")));
    }

    @Test
    void trustedPublisherShouldNotSendEmptySourceCommit(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        AtrClient client = createTrustedPublisherClient(wmRuntimeInfo, null);
        stubForGitHubOidcToken();
        stubForPublisherJwtCreate(Instant.now().plusSeconds(300).getEpochSecond());
        stubFor(post(urlEqualTo("/api/release/create"))
                .withHeader("Authorization", equalTo("Bearer project-jwt"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"release\":{\"version\":\"1.0.0\"}}")));

        client.createRelease("test-project", "1.0.0");

        verify(
                1,
                postRequestedFor(urlEqualTo("/api/publisher/jwt/create"))
                        .withRequestBody(equalToJson("{\"publisher\":\"github\",\"jwt\":\"gh-oidc-token\"}")));
    }

    @Test
    void trustedPublisherShouldExchangeNewTokenWhenExpiring(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo)
            throws Exception {
        AtrClient client = createTrustedPublisherClient(wmRuntimeInfo, null);
        stubForGitHubOidcToken();
        stubForPublisherJwtCreate(Instant.now().plusSeconds(10).getEpochSecond());
        stubForStoreWithProjectJwt();

        Path testFile = tempDir.resolve("test-file.txt");
        Files.write(testFile, "Test content".getBytes());

        client.uploadFile("test-project", "1.0.0", "test-file.txt", testFile);
        client.uploadFile("test-project", "1.0.0", "test-file.txt", testFile);

        // a GitHub OIDC token can be exchanged only once, so a new one is requested for each exchange
        verify(2, getRequestedFor(urlPathEqualTo("/github/oidc")));
        verify(2, postRequestedFor(urlEqualTo("/api/publisher/jwt/create")));
    }

    @Test
    void trustedPublisherMissingGitHubTokenShouldThrowException(
            @TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        AtrClient client = createTrustedPublisherClient(wmRuntimeInfo, null);
        stubFor(get(urlPathEqualTo("/github/oidc"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        Path testFile = tempDir.resolve("test-file.txt");
        Files.write(testFile, "Test content".getBytes());

        AtrClientException exception = assertThrows(
                AtrClientException.class, () -> client.uploadFile("test-project", "1.0.0", "test-file.txt", testFile));

        assertEquals("GitHub did not return an OIDC token", exception.getMessage());
        verify(0, postRequestedFor(urlEqualTo("/api/publisher/jwt/create")));
    }

    @Test
    void trustedPublisherRejectedExchangeShouldThrowException(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo)
            throws Exception {
        AtrClient client = createTrustedPublisherClient(wmRuntimeInfo, null);
        stubForGitHubOidcToken();
        stubFor(post(urlEqualTo("/api/publisher/jwt/create"))
                .willReturn(aResponse().withStatus(403).withBody("{\"error\": \"Self-hosted runners are rejected\"}")));

        Path testFile = tempDir.resolve("test-file.txt");
        Files.write(testFile, "Test content".getBytes());

        AtrClientException exception = assertThrows(
                AtrClientException.class, () -> client.uploadFile("test-project", "1.0.0", "test-file.txt", testFile));

        assertEquals(403, exception.getResponseCode());
        assertTrue(exception.getMessage().contains("Self-hosted runners are rejected"));
        verify(0, postRequestedFor(urlPathEqualTo("/api/release/store")));
    }

    private AtrClient createTrustedPublisherClient(WireMockRuntimeInfo wmRuntimeInfo, String sourceCommit)
            throws Exception {
        GitHubOidc gitHubOidc =
                new GitHubOidc(wmRuntimeInfo.getHttpBaseUrl() + "/github/oidc?api-version=2.0", "gh-request-token");
        return new AtrClientImpl(new URL(wmRuntimeInfo.getHttpBaseUrl()), gitHubOidc, sourceCommit);
    }

    private void stubForGitHubOidcToken() {
        stubFor(get(urlPathEqualTo("/github/oidc"))
                .withHeader("Authorization", equalTo("Bearer gh-request-token"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"count\":1,\"value\":\"gh-oidc-token\"}")));
    }

    private void stubForPublisherJwtCreate(long expires) {
        stubFor(post(urlEqualTo("/api/publisher/jwt/create"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"endpoint\":\"/publisher/jwt/create\",\"asfuid\":\"user\","
                                + "\"project\":\"test-project\",\"jwt\":\"project-jwt\",\"expires\":" + expires
                                + "}")));
    }

    private void stubForStoreWithProjectJwt() {
        stubFor(post(urlPathEqualTo("/api/release/store"))
                .withHeader("Authorization", equalTo("Bearer project-jwt"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"endpoint\":\"/release/store\",\"quarantined\":false,"
                                + "\"revision\":{\"number\":\"1\"}}")));
    }

    private void stubForReleaseCreate() {
        stubFor(post(urlEqualTo("/api/release/create"))
                .withHeader("Authorization", equalTo("Bearer test-jwt"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"release\":{\"version\":\"1.0.0\"}}")));
    }

    private void stubForJwtCreate() {
        stubFor(post(urlEqualTo("/api/jwt/create"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"jwt\":\"test-jwt\"}")));
    }
}
