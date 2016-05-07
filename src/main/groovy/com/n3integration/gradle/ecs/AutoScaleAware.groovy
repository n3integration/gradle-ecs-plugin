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

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.services.autoscaling.model.*
import com.n3integration.gradle.ecs.models.Ec2InstanceSettings

/**
 * Trait to manage auto scaling groups
 */
trait AutoScaleAware extends AWSAware {

    static final String ASG_SUFFIX = "asg-cfg"
    static final String LC_SUFFIX  = "launch-cfg"

    /**
     * Initializes a new auto scaling client
     *
     * @return a new {@link AmazonAutoScalingClient} instance
     */
    def AmazonAutoScalingClient createAutoScalingClient() {
        new AmazonAutoScalingClient(credentials())
    }

    /**
     * Creates a new ec2 launch configuration
     *
     * @param client
     *          the {@link AmazonAutoScalingClient}
     * @param settings
     *          the {@link Ec2InstanceSettings}
     * @return the launch configuration name
     */
    def String createLaunchConfiguration(AmazonAutoScalingClient client, Ec2InstanceSettings settings) {
        def cfgName = getLaunchCfgName(settings)
        client.createLaunchConfiguration(new CreateLaunchConfigurationRequest()
            .withImageId(settings.image)
            .withInstanceType(settings.instanceType)
            .withKeyName(settings.name)
            .withIamInstanceProfile(settings.iamInstanceProfileArn)
            .withSecurityGroups(settings.securityGroups)
            .withLaunchConfigurationName(cfgName)
            .withUserData(settings.userData))
        cfgName
    }

    /**
     * Deletes an ec2 launch configuration
     *
     * @param client
     *          the {@link AmazonAutoScalingClient}
     * @param settings
     *          the {@link Ec2InstanceSettings}
     */
    def void deleteLaunchConfiguration(AmazonAutoScalingClient client, Ec2InstanceSettings settings) {
        client.deleteLaunchConfiguration(new DeleteLaunchConfigurationRequest()
            .withLaunchConfigurationName(getLaunchCfgName(settings)))
    }

    /**
     * Creates a new ec2 auto scaling group
     *
     * @param client
     *          the {@link AmazonAutoScalingClient}
     * @param settings
     *          the {@link Ec2InstanceSettings}
     * @return the auto scaling group name
     */
    def String createAutoScalingGroup(AmazonAutoScalingClient client, Ec2InstanceSettings settings) {
        def cfgName = getAsgCfgName(settings)
        def autoScaling = settings.autoScaling
        client.createAutoScalingGroup(new CreateAutoScalingGroupRequest()
            .withAutoScalingGroupName(cfgName)
            .withVPCZoneIdentifier(autoScaling.subnetIds.join(","))
            .withAvailabilityZones(autoScaling.availabilityZones)
            .withLaunchConfigurationName("${settings.name}-${LC_SUFFIX}")
            .withMinSize(autoScaling.min)
            .withMaxSize(autoScaling.max)
            .withDesiredCapacity(autoScaling.min)
            .withDefaultCooldown(autoScaling.cooldownPeriod)
            .withHealthCheckType(autoScaling.healthCheck?.type)
            .withHealthCheckGracePeriod(autoScaling.healthCheck?.gracePeriod)
            .withTags(settings.tags))
        cfgName
    }

    /**
     * Scales an ec2 auto scaling group
     *
     * @param client
     *          the {@link AmazonAutoScalingClient}
     * @param settings
     *          the {@link Ec2InstanceSettings}
     */
    def void scaleAutoScalingGroup(AmazonAutoScalingClient client, Ec2InstanceSettings settings) {
        client.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
            .withAutoScalingGroupName(getAsgCfgName(settings))
            .withMinSize(settings.autoScaling?.min)
            .withMaxSize(settings.autoScaling?.max)
            .withDesiredCapacity(settings.autoScaling?.min))
    }

    /**
     * Waits for all auto scaling groups ec2 instances to terminate
     *
     * @param client
     *          the {@link AmazonAutoScalingClient}
     * @param settings
     *          the {@link Ec2InstanceSettings}
     * @param timeout
     *          the time to wait between requests
     */
    def void waitForInstancesToTerminate(AmazonAutoScalingClient client, Ec2InstanceSettings settings, long timeout) {
        def count = 1
        while(count > 0) {
            sleep(timeout)

            def result = client.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest()
                .withAutoScalingGroupNames(getAsgCfgName(settings)))

            count = result.autoScalingGroups.collect {
                it.instances.size()
            }.inject(0) { sum, i ->
                sum + i
            }
        }
    }

    /**
     * Deletes an ec2 auto scaling group
     *
     * @param client
     *          the {@link AmazonAutoScalingClient}
     * @param settings
     *          the {@link Ec2InstanceSettings}
     */
    def void deleteAutoScalingGroup(AmazonAutoScalingClient client, Ec2InstanceSettings settings) {
        client.deleteAutoScalingGroup(new DeleteAutoScalingGroupRequest()
            .withAutoScalingGroupName(getAsgCfgName(settings)))
    }

    private String getLaunchCfgName(Ec2InstanceSettings settings) {
        "${settings.name}-${LC_SUFFIX}"
    }

    private String getAsgCfgName(Ec2InstanceSettings settings) {
        "${settings.name}-${ASG_SUFFIX}"
    }
}