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
package com.n3integration.gradle.aws

import com.amazonaws.regions.Regions
import com.n3integration.gradle.aws.models.Cluster
import com.n3integration.gradle.aws.models.Credentials
import com.n3integration.gradle.aws.models.SecurityTokenCredentials
import org.gradle.api.NamedDomainObjectContainer

/**
 * Gradle {@code aws} extension to simplify the configuration of AWS
 * orchestration
 */
class AWSExtension {

    final NamedDomainObjectContainer<Cluster> clusters

    String profile
    String region = Regions.US_EAST_1.name

    Cluster defaultCluster

    Credentials credentials
    SecurityTokenCredentials securityTokenCredentials

    AWSExtension(clusters) {
        this.clusters = clusters
    }

    void credentials(Closure closure) {
        this.credentials = new Credentials()
        this.credentials.accessKey = closure.getProperty("accessKey")
        this.credentials.secretKey = closure.getProperty("secretKey")
    }

    void securityTokenCredentials(Closure closure) {
        this.securityTokenCredentials = new SecurityTokenCredentials()
        this.securityTokenCredentials.roleArn  = closure.getProperty("roleArn")
        this.securityTokenCredentials.accessKey = closure.getProperty("accessKey")
        this.securityTokenCredentials.secretKey = closure.getProperty("secretKey")
        this.securityTokenCredentials.sessionToken = closure.getProperty("sessionToken")
    }

    void clusters(Closure closure) {
        clusters.configure(closure)
    }
}
