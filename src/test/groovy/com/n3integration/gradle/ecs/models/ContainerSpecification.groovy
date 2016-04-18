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

import com.amazonaws.services.ecs.model.ContainerDefinition
import com.amazonaws.services.ecs.model.KeyValuePair
import com.amazonaws.services.ecs.model.PortMapping
import spock.lang.Specification
import spock.lang.Unroll

class ContainerSpecification extends Specification {

    public static final String ENV_KEY = "MYSQL_ROOT_PASSWORD"
    public static final String ENV_VAL = "password"
    public static final String WORDPRESS = "wordpress"
    public static final String MYSQL = "mysql"
    public static final int FIVEHUNDRED_MIB = 500
    public static final int HTTP_PORT = 80
    public static final int TEN = 10

    @Unroll
    def "#container should be equivalent to #definition"() {
        expect:
            container.toDefinition() == definition

        where:
            container               | definition
            wordpressContainer()    | expectedWordpressDefinition()
            mysqlContainer()        | expectedMySqlDefinition()
    }

    private Container wordpressContainer() {
        def container = new Container(WORDPRESS)
        container.links = [MYSQL]
        container.image = WORDPRESS
        container.ports = ["80:80"]
        container.memory =  FIVEHUNDRED_MIB
        container.cpu = TEN
        container
    }

    private Container mysqlContainer() {
        def container = new Container(MYSQL)
        container.environment = [(ENV_KEY):ENV_VAL]
        container.image = MYSQL
        container.cpu = TEN
        container.memory = FIVEHUNDRED_MIB
        container
    }

    private ContainerDefinition expectedWordpressDefinition() {
        new ContainerDefinition().withName(WORDPRESS)
            .withLinks(MYSQL)
            .withImage(WORDPRESS)
            .withPortMappings(new PortMapping().withHostPort(HTTP_PORT).withContainerPort(HTTP_PORT))
            .withMemory(FIVEHUNDRED_MIB)
            .withCpu(TEN)
    }

    private ContainerDefinition expectedMySqlDefinition() {
        new ContainerDefinition().withName(MYSQL)
            .withEnvironment(new KeyValuePair().withName(ENV_KEY).withValue(ENV_VAL))
            .withImage(MYSQL)
            .withCpu(TEN)
            .withMemory(FIVEHUNDRED_MIB)
    }
}
