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

# Apache Tooling ATR Maven Plugin

The ATR Maven Plugin is used to upload Apache release artifacts to [ATR (Apache Trusted Releases)][introduction-to-atr] before starting a vote.

## Goals Overview

The ATR Plugin has 3 goals:

* [atr:check-composing](./check-composing-mojo.html) checks if a version exists in ATR and is being composed (in draft phase),
* [atr:release-start](./release-start-mojo.html) create a composed (in draft phase) version in ATR if it does not exist yet,
* [atr:upload](./upload-mojo.html) uploads distribution artifacts to ATR compose space.

## Usage

General instructions on how to use the ATR Maven Plugin can be found on the [usage page](./usage.html).

In case you still have questions regarding the plugin's usage, please feel free to contact the [user mailing list](./mailing-lists.html). The posts to the mailing list are archived and could already contain the answer to your question as part of an older thread. Hence, it is also worth browsing/searching the [mail archive](./mailing-lists.html).

If you feel like the plugin is missing a feature or has a bug, you can fill a feature request or bug report in our [issue tracker](./issue-management.html). When creating a new issue, please provide a comprehensive description of your concern. Especially for fixing bugs it is crucial that the developers can reproduce your problem. For this reason, entire debug logs, POMs or most preferably little demo projects attached to the issue are very much appreciated.

Of course, patches are welcome, too. Contributors can check out the project from our [source repository](./scm.html) and will find supplementary information in the [guide to helping with Maven](https://maven.apache.org/guides/development/guide-helping.html).

## What is ATR?

[ATR (Apache Trusted Releases)][introduction-to-atr] is a platform through which committees of Apache Software Foundation (ASF) projects can make official ASF software releases.

[introduction-to-atr]: https://release-test.apache.org/docs/introduction-to-atr
