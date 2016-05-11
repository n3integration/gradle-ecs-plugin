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

import com.n3integration.gradle.ecs.ECSAware
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
        def cluster = project.ecs.clusters.findByName(cluster)
        action(createEcsClient(), cluster)
    }

    private void ensureClusterName() {
        if (cluster == null) {
            if (defaultClusterProvided()) {
                cluster = project.ecs.defaultCluster.name
            } else {
                throw new GradleException("missing cluster; a defaultCluster or task cluster is required")
            }
        }
    }

    private boolean defaultClusterProvided() {
        project.ecs && project.ecs.defaultCluster
    }
}
