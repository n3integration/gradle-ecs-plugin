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

import com.n3integration.gradle.ecs.models.Cluster
import com.n3integration.gradle.ecs.models.Container
import com.n3integration.gradle.ecs.tasks.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * Gradle plugin to configure an ec2 container service cluster
 *
 * @author n3integration
 */
class ECSPlugin implements Plugin<Project> {

    private static Logger logger = Logging.getLogger(ECSPlugin)

    public static final String ECS_EXTENSION = "ecs"
    public static final String AWS_SDK_VERSION = "1.10.69"
    public static final String AWS_JAVA_CONFIGURATION_NAME = "awsJava"

    @Override
    void apply(Project project) {
        def clusters = project.container(Cluster)
        clusters.all {
            containers = project.container(Container)
        }

        addCreateClusterTaskType(project)
        addUpTaskType(project)
        addScaleTaskType(project)
        addDownTaskType(project)
        addDeleteClusterTaskType(project)

        project.extensions.create(ECS_EXTENSION, ECSExtension, clusters)
        project.configurations.create(AWS_JAVA_CONFIGURATION_NAME)
            .setVisible(false)
            .setTransitive(true)
            .setDescription('The AWS SDK to be used for this project.')

        Configuration config = project.configurations[AWS_JAVA_CONFIGURATION_NAME]
        config.defaultDependencies { dependencies ->
            dependencies.add(project.dependencies.create("com.amazonaws:aws-java-sdk-autoscaling:${AWS_SDK_VERSION}"))
            dependencies.add(project.dependencies.create("com.amazonaws:aws-java-sdk-config:${AWS_SDK_VERSION}"))
            dependencies.add(project.dependencies.create("com.amazonaws:aws-java-sdk-core:${AWS_SDK_VERSION}"))
            dependencies.add(project.dependencies.create("com.amazonaws:aws-java-sdk-ec2:${AWS_SDK_VERSION}"))
            dependencies.add(project.dependencies.create("com.amazonaws:aws-java-sdk-ecs:${AWS_SDK_VERSION}"))
            dependencies.add(project.dependencies.create("com.amazonaws:aws-java-sdk-iam:${AWS_SDK_VERSION}"))
            dependencies.add(project.dependencies.create("com.amazonaws:aws-java-sdk-sts:${AWS_SDK_VERSION}"))
            dependencies.add(project.dependencies.create('org.slf4j:slf4j-simple:1.7.5'))
        }
    }

    private void addCreateClusterTaskType(Project project) {
        logger.info("Adding create cluster task type")
        project.ext.CreateCluster = CreateClusterTask.class
    }

    private void addUpTaskType(Project project) {
        logger.info("Adding up task type")
        project.ext.Up = UpTask.class
    }

    private void addScaleTaskType(Project project) {
        logger.info("Adding scale task type")
        project.ext.Scale = ScaleTask.class
    }

    private void addDownTaskType(Project project) {
        logger.info("Adding down task type")
        project.ext.Down = DownTask.class
    }

    private void addDeleteClusterTaskType(Project project) {
        logger.info("Adding delete cluster task type")
        project.ext.DeleteCluster = DeleteClusterTask.class
    }
}
