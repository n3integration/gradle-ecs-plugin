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

import com.amazonaws.services.autoscaling.model.Tag
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification
import com.google.common.collect.Lists
import com.google.common.io.BaseEncoding
import groovy.text.SimpleTemplateEngine

import java.nio.charset.Charset

class Ec2InstanceSettings {

    static final String DEFAULT_AMI     = "ami-6d1c2007"    // centos 7
    static final String DEFAULT_TYPE    = "t2.micro"        // free tier

    String name
    String image = DEFAULT_AMI
    String instanceType = DEFAULT_TYPE
    String subnet
    String userData
    String iamInstanceProfileArn
    Scaling scale
    AutoScaling autoScaling
    List<String> securityGroups
    List<Tag> tags

    Ec2InstanceSettings(String name) {
        this.name = name
        this.tags = Lists.newArrayList()
        this.userData = loadUserDataTemplate(name)
    }

    def IamInstanceProfileSpecification instanceProfileSpecification() {
        if(iamInstanceProfileArn) {
            return new IamInstanceProfileSpecification()
                .withArn(iamInstanceProfileArn)
        }
        return null
    }

    def tag(@DelegatesTo(Tag) Closure closure) {
        def tag = new Tag()
        def clone = closure.rehydrate(tag, this, this)
        clone.resolveStrategy = Closure.DELEGATE_ONLY
        clone()
        this.tags.add(tag)
    }

    def scale(@DelegatesTo(Scaling) Closure closure) {
        this.scale = new Scaling()
        def clone = closure.rehydrate(scale, this, this)
        clone.resolveStrategy = Closure.DELEGATE_ONLY
        clone()
    }

    def autoScaling(@DelegatesTo(AutoScaling) Closure closure) {
        this.autoScaling = new AutoScaling()
        def clone = closure.rehydrate(autoScaling, this, this)
        clone.resolveStrategy = Closure.DELEGATE_ONLY
        clone()
    }

    static String loadUserDataTemplate(String name) {
        def binding = [name:name, proxy:proxy()]
        def engine = new SimpleTemplateEngine()
        def template = engine.createTemplate(Ec2InstanceSettings.class.getResource("/userdata.sh").text)
            .make(binding)
        BaseEncoding.base64().encode(template.toString().getBytes(Charset.defaultCharset()))
    }

    static String proxy() {
        return System.getenv("HTTP_PROXY") ? System.getenv("HTTP_PROXY")
            : System.getProperty("http.proxy")
    }
}
