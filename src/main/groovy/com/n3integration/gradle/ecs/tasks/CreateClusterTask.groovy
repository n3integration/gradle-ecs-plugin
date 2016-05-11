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
import com.n3integration.gradle.ecs.AutoScaleAware
import com.n3integration.gradle.ecs.Ec2Aware
import com.n3integration.gradle.ecs.models.Cluster
import org.gradle.api.tasks.TaskAction

/**
 * Creates an ECS cluster
 *
 * @author n3integration
 */
class CreateClusterTask extends DefaultClusterTask implements Ec2Aware, AutoScaleAware {

    CreateClusterTask() {
        this.description = "Creates a new EC2 Container Service cluster"
    }

    @TaskAction
    def createClusterAction() {
        def asClient = createAutoScalingClient()
        super.execute { AmazonECSClient ecsClient, Cluster _cluster ->
            logger.quiet("Creating ${cluster} cluster...")
            def result = createCluster(ecsClient, _cluster)
            logger.quiet("\tcluster: ${cluster}")
            logger.quiet("\t status: ${result.cluster?.status}")

            def instanceSettings = _cluster.instanceSettings
            if(instanceSettings && instanceSettings.autoScaling) {
                def ec2Client = createEc2Client(_cluster)
                logger.quiet("Creating key pair...")
                result = createKeyPairIfNecessary(ec2Client, _cluster)
                logger.quiet("\tkey: ${result}")

                logger.quiet("Creating launch configuration...")
                result = createLaunchConfiguration(asClient, instanceSettings)
                logger.quiet("\tcreated config: ${result}")

                logger.quiet("Creating auto scaling group...")
                result = createAutoScalingGroup(asClient, instanceSettings)
                logger.quiet("\tcreated group: ${result}")
            }
        }
    }
}
