/*
 *  Copyright 2016 n3integration
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.n3integration.gradle.aws.tasks

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import org.gradle.api.DefaultTask

class DefaultAWSTask extends DefaultTask {

    AWSCredentials credentials

    def AWSCredentials getCredentials() {
        if(credentials == null) {
            if(project.aws.credentials) {
                credentials = project.aws.credentials.toCredentials()
            }
            else {
                def credentialsProvider = new DefaultAWSCredentialsProviderChain()
                credentials = credentialsProvider.getCredentials()
            }
        }
        credentials
    }
}
