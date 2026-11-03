<!---
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Usage

## Configuring Credentials

The ATR Maven Plugin uses Maven's standard authentication mechanism. Configure your ATR credentials in `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>apache.atr</id>
      <username>your-asf-id</username>
      <password>your-personal-access-token (encrypted)</password>
    </server>
  </servers>
</settings>
```

The username should be your ASF user ID and the password should be your Personal Access Token (PAT) from the ATR service.

For security, you should encrypt your password using [Maven's password encryption](https://maven.apache.org/guides/mini/guide-encryption.html).

## Basic Upload

To upload release artifacts to ATR:

```
mvn org.apache.tooling:atr-maven-plugin:upload \
  -Datr.files=target/apache-maven-4.0.0-src.tar.gz,target/apache-maven-4.0.0-src.tar.gz.sha512
```

## Integration ATR plugin with the push-to-atr profile

Since version 39, the [ASF parent POM](https://maven.apache.org/pom/asf/) provides
a [`push-to-atr` profile](https://maven.apache.org/pom/asf/#The_push-to-atr_Profile),
to be used together with the [`apache-release` profile](https://maven.apache.org/pom/asf/#The_apache-release_Profile).
It runs the `check-composing` goal and uploads the source release archive with its `.sha512` and `.asc` files.

If your project inherits from the ASF parent POM, you don't need to define this profile yourself.
You can test the upload of the current project version by running:

```
mvn clean deploy -Papache-release,push-to-atr
```

or during the release process:

```
mvn release:prepare
mvn release:perform -Ppush-to-atr
```

**NOTE**: The `check-composing` goal will check if the version is already being composed in ATR.

You need to manually create a new version in ATR service before using the `push-to-atr` profile,
or let the `check-composing` goal create it, see [Creating the version in ATR](#Creating_the_version_in_ATR).

### Customizing the push-to-atr profile

The archive type of the source release is set by the `atr-release-type` property (`zip` by default).

When the ATR project name differs from `${project.artifactId}`, set it in `pluginManagement`,
so it is used by every execution of the plugin, also outside the profile:

```xml
<build>
  <pluginManagement>
    <plugins>
      <plugin>
        <groupId>org.apache.tooling</groupId>
        <artifactId>atr-maven-plugin</artifactId>
        <configuration>
          <project>my-atr-project</project>
        </configuration>
      </plugin>
    </plugins>
  </pluginManagement>
</build>
```

The profile can be extended in your project, for example to upload additional files:

```xml
<profiles>
  <profile>
    <id>push-to-atr</id>
    <build>
      <plugins>
        <plugin>
          <groupId>org.apache.tooling</groupId>
          <artifactId>atr-maven-plugin</artifactId>
          <executions>
            <execution>
              <id>upload-to-atr</id>
              <configuration>
                <files combine.children="append">
                  <file>${project.build.directory}/${project.artifactId}-${project.version}-cyclonedx.json</file>
                </files>
              </configuration>
            </execution>
          </executions>
        </plugin>
      </plugins>
    </build>
  </profile>
</profiles>
```

#### Creating the version in ATR

The `check-composing` goal can create the version in ATR when it does not exist yet,
with the `createVersion` parameter. Enable it for the `atr-check-composing` execution in `pluginManagement`:

```xml
<build>
  <pluginManagement>
    <plugins>
      <plugin>
        <groupId>org.apache.tooling</groupId>
        <artifactId>atr-maven-plugin</artifactId>
        <executions>
          <execution>
            <id>atr-check-composing</id>
            <configuration>
              <createVersion>true</createVersion>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </pluginManagement>
</build>
```

or from the command line with `-Datr.createVersion`.

## Directory Organization

You can organize uploaded files in subdirectories with the `directory` parameter.

For example, to upload binary distributions to the `binaries/` directory of the compose space,
add a separate `upload` execution to the `push-to-atr` profile:

```xml
<profiles>
  <profile>
    <id>push-to-atr</id>
    <build>
      <plugins>
        <plugin>
          <groupId>org.apache.tooling</groupId>
          <artifactId>atr-maven-plugin</artifactId>
          <executions>
            <execution>
              <id>upload-bin-to-atr</id>
              <goals>
                <goal>upload</goal>
              </goals>
              <configuration>
                <directory>binaries</directory>
                <files>
                  <file>${project.build.directory}/${project.artifactId}-${project.version}-bin.tar.gz</file>
                  <file>${project.build.directory}/${project.artifactId}-${project.version}-bin.tar.gz.sha512</file>
                  <file>${project.build.directory}/${project.artifactId}-${project.version}-bin.tar.gz.asc</file>
                </files>
              </configuration>
            </execution>
          </executions>
        </plugin>
      </plugins>
    </build>
  </profile>
</profiles>
```

The source release is still uploaded by the `upload-to-atr` execution from the ASF parent POM.
