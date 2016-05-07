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

import org.gradle.api.NamedDomainObjectContainer

/**
 * ECS cluster model definition
 *
 * @author n3integration
 */
class Cluster {

    static final String DEFAULT_NAME    = "default"
    static final String DEFAULT_REGION  = "us-east-1"

    String name
    String region = DEFAULT_REGION
    Ec2InstanceSettings instanceSettings
    NamedDomainObjectContainer<Container> containers

    Cluster() {
        this(DEFAULT_NAME)
    }

    Cluster(String name) {
        this.name = name
        this.instanceSettings = new Ec2InstanceSettings(name)
    }

    Cluster(String name, containers) {
        this.name = name
        this.containers = containers
        this.instanceSettings = new Ec2InstanceSettings(name)
    }

    void containers(Closure closure) {
        containers.configure(closure)
    }

    void instanceSettings(@DelegatesTo(Ec2InstanceSettings) Closure closure) {
        this.instanceSettings = new Ec2InstanceSettings(name)
        def clone = closure.rehydrate(instanceSettings, this, this)
        clone.resolveStrategy = Closure.DELEGATE_ONLY
        clone()
    }

    def List<String> families() {
        containers.collect { container ->
            "${name}-${container.familySuffix()}"
        }
    }
}
