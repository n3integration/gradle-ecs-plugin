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
package com.n3integration.gradle.ecs.models

import com.google.common.collect.Lists

/**
 * Auto scaling group definition
 *
 * @author n3integration
 */
class AutoScaling {

    static final int DEFAULT_MIN        = 1
    static final int DEFAULT_COOLDOWN   = 300       // five minutes

    Integer min = DEFAULT_MIN
    Integer max = DEFAULT_MIN * 2
    Integer cooldownPeriod = DEFAULT_COOLDOWN
    HealthCheck healthCheck
    List<String> subnetIds
    List<String> availabilityZones

    AutoScaling() {
        this.healthCheck = new HealthCheck()
        this.subnetIds = Lists.newArrayList()
        this.availabilityZones = Lists.newArrayList()
    }

    def healthCheck(@DelegatesTo(HealthCheck) Closure closure) {
        this.healthCheck = new HealthCheck()
        def clone = closure.rehydrate(healthCheck, this, this)
        clone.resolveStrategy = Closure.DELEGATE_ONLY
        clone()
    }
}
