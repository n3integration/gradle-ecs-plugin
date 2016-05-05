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

import com.amazonaws.services.ecs.AmazonECSClient
import com.amazonaws.services.ecs.model.ClusterNotFoundException
import com.n3integration.gradle.ecs.AutoScaleAware
import com.n3integration.gradle.ecs.Ec2Aware
import com.n3integration.gradle.ecs.models.Cluster
import org.gradle.api.tasks.TaskAction

class DeleteCluster extends DefaultClusterTask implements AutoScaleAware, Ec2Aware {

    DeleteCluster() {
        this.description = "Deletes an EC2 Container Service cluster"
    }

    @TaskAction
    def deleteClusterAction() {
        def asClient = createAutoScalingClient()
        super.execute { AmazonECSClient ecsClient, Cluster cluster ->
            try {
                def result
                def instanceSettings = cluster.instanceSettings
                if(instanceSettings && instanceSettings.autoScaling) {
                    logger.quiet("Scaling down auto scaling group...")
                    instanceSettings.autoScaling.min = 0
                    scaleAutoScalingGroup(asClient, instanceSettings)
                    waitForInstancesToTerminate(asClient, instanceSettings, 10000)
                    logger.quiet("Deleting auto scaling group...")
                    deleteAutoScalingGroup(asClient, instanceSettings)
                    logger.quiet("Deleting launch configuration...")
                    deleteLaunchConfiguration(asClient, instanceSettings)
                }
                else if(instanceSettings && instanceSettings.scale) {
                    def instances = listContainerInstances(ecsClient, cluster)
                    if(!instances.isEmpty()) {
                        def ec2Client = createEc2Client(cluster)
                        logger.quiet("Terminating instances...")
                        result = deleteEc2Instances(ec2Client, instances.collect { it.ec2InstanceId })
                        result.terminatingInstances.each { instance ->
                            logger.quiet("\tinstance: ${instance.instanceId}")
                            logger.quiet("\t   state: ${instance.currentState}")
                        }
                    }
                }
                logger.quiet("Deleting ${clusterName} cluster...")
                result = deleteCluster(ecsClient, cluster)
                logger.debug("${clusterName}:${result.cluster?.status}")
            }
            catch(ClusterNotFoundException ex) {
                logger.warn("${clusterName} not found")
            }
        }
    }
}
