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
  -Datr.project=maven \
  -Datr.version=4.0.0 \
  -Datr.files=apache-maven-4.0.0-src.tar.gz,apache-maven-4.0.0-src.tar.gz.sha512
```

## Integration with Release Profile

The typical use case is to integrate the ATR plugin into a `push-to-atr` profile that will be used optionally with `apache-release` profile:

```xml
<project>
  ...
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.tooling</groupId>
        <artifactId>atr-maven-plugin</artifactId>
        <version>1.0.0-beta-1-SNAPSHOT</version>
      </plugin>
    </plugins>
  </build>

  <profiles>
    <profile>
      <id>push-to-atr</id>
      <build>
        <plugins>
          <plugin>
            <groupId>org.apache.tooling</groupId>
            <artifactId>atr-maven-plugin</artifactId>
            <configuration>
              <project>${project.artifactId}</project>
              <version>${project.version}</version>
            </configuration>
            <executions>
              <execution>
                <id>atr-check-composing</id>
                <goals>
                  <goal>check-composing</goal>
                </goals>
              </execution>
              <execution>
                <id>upload-to-atr</id>
                <goals>
                  <goal>upload</goal>
                </goals>
                <configuration>
                  <files>
                    <file>${project.build.directory}/${project.artifactId}-${project.version}-source-release.zip</file>
                    <file>${project.build.directory}/${project.artifactId}-${project.version}-source-release.zip.sha512</file>
                    <file>${project.build.directory}/${project.artifactId}-${project.version}-source-release.zip.asc</file>
                  </files>
                </configuration>
              </execution>
            </executions>
          </plugin>
        </plugins>
      </build>
    </profile>
  </profiles>
</project>
```

Then during the release process:

```
mvn clean deploy -Papache-release,push-to-atr
```

## Directory Organization

You can organize uploaded files in subdirectories:

```xml
<configuration>
  <directory>binaries</directory>
  <files>
    <file>${project.build.directory}/${project.artifactId}-${project.version}-bin.tar.gz</file>
  </files>
</configuration>
```

This will upload the file to the compose space at: `binaries/`.
