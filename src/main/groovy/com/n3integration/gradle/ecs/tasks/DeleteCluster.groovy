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

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.services.ecs.AmazonECSClient
import com.amazonaws.services.ecs.model.ClusterNotFoundException
import com.n3integration.gradle.ecs.AutoScaleAware
import com.n3integration.gradle.ecs.Ec2Aware
import com.n3integration.gradle.ecs.models.Cluster
import org.gradle.api.tasks.TaskAction

/**
 * Deletes an ECS cluster and terminates any ec2 instances registered with the
 * cluster or part of the auto scaling group
 *
 * @author n3integration
 */
class DeleteCluster extends DefaultClusterTask implements AutoScaleAware, Ec2Aware {

    public static final int RETRY_DELAY = 10000

    DeleteCluster() {
        this.description = "Deletes an EC2 Container Service cluster"
    }

    @TaskAction
    def deleteClusterAction() {
        def asClient = createAutoScalingClient()
        super.execute { AmazonECSClient ecsClient, Cluster cluster ->
            try {
                terminateEc2Instances(asClient, cluster)
                logger.quiet("Deleting ${clusterName} cluster...")
                def result = deleteCluster(ecsClient, cluster)
                logger.debug("${clusterName}:${result.cluster?.status}")
            }
            catch(ClusterNotFoundException ex) {
                logger.warn("${clusterName} not found")
            }
        }
    }

    def terminateEc2Instances(AmazonAutoScalingClient client, Cluster cluster) {
        def instanceSettings = cluster.instanceSettings
        if (instanceSettings && instanceSettings.autoScaling) {
            logger.quiet("Bringing down auto scaling group...")
            instanceSettings.autoScaling.min = 0
            scaleAutoScalingGroup(client, instanceSettings)
            waitForInstancesToTerminate(client, instanceSettings, RETRY_DELAY)

            logger.quiet("Deleting auto scaling group...")
            deleteAutoScalingGroup(client, instanceSettings)

            logger.quiet("Deleting launch configuration...")
            deleteLaunchConfiguration(client, instanceSettings)
        }
    }
}
