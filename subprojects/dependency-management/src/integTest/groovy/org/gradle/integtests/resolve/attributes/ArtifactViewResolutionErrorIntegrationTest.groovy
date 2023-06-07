/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.integtests.resolve.attributes

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
/**
 * Tests that resolution failures caused by {@link org.gradle.api.artifacts.ArtifactView ArtifactView}
 * rather than configurations indicate this in the error message.
 */
class ArtifactViewResolutionErrorIntegrationTest extends AbstractIntegrationSpec {
    def setup() {
        settingsFile << """
            rootProject.name = "consumer"
            include "producer"
        """

        file("producer/build.gradle.kts") << """
            val flavor: Attribute<String> = Attribute.of("flavor", String::class.java)

            configurations {
                register("producerConfVanilla") {
                    attributes.attribute(flavor, "vanilla")
                    outgoing.artifact(file("vanilla.jar"))
                }
                register("producerConfChocolate") {
                    attributes.attribute(flavor, "chocolate")
                    outgoing.artifact(file("chocolate.jar"))
                }
            }
        """

        buildKotlinFile << """
            val flavor: Attribute<String> = Attribute.of("flavor", String::class.java)

            configurations {
                register("consumerConf") {
                    attributes.attribute(flavor, "cinnamon")
                }
            }

            dependencies {
                configurations["consumerConf"](project(":producer"))
            }
        """
    }

    def "artifact view resolution error mentions artifact view with default name"() {
        buildKotlinFile << """
            tasks.register("verifyFiles") {
                val artifactViewFiles = configurations.named("consumerConf").get().incoming.artifactView { }.files

                doLast {
                    artifactViewFiles.forEach { it.exists() } // Force resolution
                }
            }
        """

        expect:
        fails("verifyFiles")
        failureCauseContains("Could not resolve all files for artifact view for configuration ':consumerConf'.")
        !errorOutput.contains("Could not resolve all files for configuration ':consumerConf'.")
    }

    def "artifact view resolution error mentions artifact view with custom display name"() {
        buildKotlinFile << """
            tasks.register("verifyFiles") {
                val artifactViewFiles = configurations.named("consumerConf").get().incoming.artifactView { displayName = "tasty view" }.files

                doLast {
                    artifactViewFiles.forEach { it.exists() } // Force resolution
                }
            }
        """

        expect:
        fails("verifyFiles")
        failureCauseContains("Could not resolve all files for artifact view: 'tasty view' for configuration ':consumerConf'.")
        !errorOutput.contains("Could not resolve all files for configuration ':consumerConf'.")
    }

    def "configuration resolution error does not mention artifact view"() {
        buildKotlinFile << """
            tasks.register("verifyFiles") {
                val incomingFiles = configurations.named("consumerConf").get().incoming.files

                doLast {
                    incomingFiles.forEach { it.exists() } // Force resolution
                }
            }
        """

        expect:
        fails("verifyFiles")
        failureCauseContains("Could not resolve all files for configuration ':consumerConf'.")
        !errorOutput.contains("Could not resolve all files for artifact view for configuration ':consumerConf'.")
        !errorOutput.contains("Could not resolve all files for artifact view: 'tasty view' for configuration ':consumerConf'.")
    }
}