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
package com.n3integration.gradle.ecs

import com.amazonaws.regions.Regions
import com.n3integration.gradle.ecs.models.Cluster
import com.n3integration.gradle.ecs.models.Credentials
import com.n3integration.gradle.ecs.models.SecurityTokenCredentials
import org.gradle.api.NamedDomainObjectContainer

/**
 * Gradle {@code aws} extension to simplify the configuration of ECS
 * orchestration
 *
 * @author n3integration
 */
class ECSExtension {

    final NamedDomainObjectContainer<Cluster> clusters

    String profile
    String region = Regions.US_EAST_1.name

    Cluster defaultCluster

    Credentials credentials
    SecurityTokenCredentials securityTokenCredentials

    ECSExtension(clusters) {
        this.clusters = clusters
    }

    void credentials(@DelegatesTo(Credentials) Closure closure) {
        this.credentials = new Credentials()
        def clone = closure.rehydrate(credentials, this, this)
        clone.resolveStrategy = Closure.DELEGATE_ONLY
        clone()
    }

    void securityTokenCredentials(@DelegatesTo(SecurityTokenCredentials) Closure closure) {
        this.securityTokenCredentials = new SecurityTokenCredentials()
        def clone = closure.rehydrate(securityTokenCredentials, this, this)
        clone.resolveStrategy = Closure.DELEGATE_ONLY
        clone()
    }

    void clusters(Closure closure) {
        clusters.configure(closure)
    }
}
