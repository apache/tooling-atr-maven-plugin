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

// verification after first goal - release:perform was only executed

def buildLog = new File(basedir, 'build.log')
assert buildLog.exists()

// release preparation finished
assert buildLog.text.contains('prepare:run-preparation-goals') :
        'Expected message not found in build log'

assert !buildLog.text.contains('perform:run-perform-goals') :
        'Release perform should not be performed during release preparation'

// there are no interactions with ATR client during release preparation,
// as the profile apache-release is not active
assert !buildLog.text.contains('[Mock ATR client]') :
        'Should be no interaction with ATR client during release preparation'


