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

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.n3integration.gradle.ecs.models.Cluster

/**
 * Defines common AWS criteria
 *
 * @author n3integration
 */
trait AWSAware {

    /**
     * Constructs a {@link Regions} instance from the {@code region} defined in the
     * {@code cluster}.
     *
     * @param cluster
     *          the {@link Cluster} definition
     * @return the associated {@link Regions} instance
     */
    def Regions getRegion(Cluster cluster) {
        if(cluster) {
            Regions.fromName(cluster?.region ?: project.ecs.region)
        }
        else {
            Regions.US_EAST_1
        }
    }

    /**
     * Fetches the default {@link AWSCredentials}
     *
     * @return initialized {@link AWSCredentials} instance
     */
    def AWSCredentials defaultCredentials() {
        def credentialsProvider = new DefaultAWSCredentialsProviderChain()
        credentialsProvider.getCredentials()
    }

    /**
     * Attempts to retrieve the {@link AWSCredentials} from {@code this}
     *
     * @return {@code this.getCredentials()} or {@link #defaultCredentials()}
     */
    def AWSCredentials credentials() {
        if(this.class.metaClass.respondsTo(this, "getCredentials")) {
            return getCredentials()
        }
        else {
            return defaultCredentials()
        }
    }
}
