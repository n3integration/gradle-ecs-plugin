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
import com.n3integration.gradle.ecs.ECSAware
import com.n3integration.gradle.ecs.models.Cluster
import org.gradle.api.GradleException

/**
 * Base implementation for ECS tasks
 */
class DefaultClusterTask extends DefaultAWSTask implements ECSAware {

    String cluster

    DefaultClusterTask() {
        this.group = "EC2 Container Service"
    }

    def execute(action) {
        ensureClusterName()
        Cluster _cluster = getTaskCluster()
        action(createEcsClient(_cluster), _cluster)
    }

    def void ensureClusterName() {
        if (Strings.isNullOrEmpty(cluster)) {
            if (defaultClusterProvided()) {
                this.cluster = project.ecs.defaultCluster.name
            }
            else {
                throw new GradleException("a task cluster is required")
            }
        }
    }

    def boolean defaultClusterProvided() {
        project.ecs && project.ecs.defaultCluster
    }

    def Cluster getTaskCluster() {
        try {
            return project.ecs.clusters.findByName(cluster)
        }
        catch(Exception e) {
            return new Cluster(cluster)
        }
    }
}
