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
package com.n3integration.gradle.ecs.tasks

import com.google.common.base.Strings
import com.n3integration.gradle.ecs.AutoScaleAware
import com.n3integration.gradle.ecs.Ec2Aware
import com.n3integration.gradle.ecs.models.Cluster
import com.n3integration.gradle.ecs.models.Container
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

/**
 * Responsible for scaling the number of container instances, or groups
 *
 * @author n3integration
 */
class ScaleTask extends DefaultClusterTask implements AutoScaleAware, Ec2Aware {

    Container container

    ScaleTask() {
        this.description = "Scales the number of running containers or groups"
    }

    @TaskAction
    def scaleClusterAction() {
        ensureClusterName()

        if(container) {
            if(Strings.isNullOrEmpty(container.name) && Strings.isNullOrEmpty(container.group)) {
                throw new GradleException("a container name or group is required for container scaling")
            }
        }
        else {
            throw new GradleException("a container definition is required for scaling")
        }

        Cluster cluster = project.ecs.clusters.findByName(cluster)

        logger.quiet("Scaling ${serviceName()} service...")
        def ecsClient = createEcsClient(cluster)
        scaleServices(ecsClient, cluster.name, Collections.singletonList(serviceName()), container.instances)
    }

    def container(@DelegatesTo(Container) Closure closure) {
        this.container = new Container()
        def clone = closure.rehydrate(this.container, this, this)
        clone.resolveStrategy = Closure.DELEGATE_ONLY
        clone()
    }

    private String serviceName() {
        if(Strings.isNullOrEmpty(container.group)) {
            return container.name
        }
        return container.group
    }
}
