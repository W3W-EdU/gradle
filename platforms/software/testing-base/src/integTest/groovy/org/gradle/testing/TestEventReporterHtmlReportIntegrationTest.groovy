/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.testing

import org.gradle.api.internal.tasks.testing.report.generic.GenericHtmlTestExecutionResult
import org.gradle.api.internal.tasks.testing.report.generic.GenericTestExecutionResult
import org.gradle.api.tasks.testing.TestResult
import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.internal.logging.ConsoleRenderer

import static org.gradle.util.Matchers.containsText
import static org.hamcrest.CoreMatchers.equalTo

class TestEventReporterHtmlReportIntegrationTest extends AbstractIntegrationSpec {

    def "successful tests do not emit HTML reports to console"() {
        given:
        buildFile << passingTask("passing")

        when:
        succeeds("passing")

        then:
        outputDoesNotContain("See the test results for more details")
        outputDoesNotContain("Aggregate test results")

        // Aggregate results are still emitted even if we don't print the URL to console
        aggregateResults()
            .testPath(":passing suite")
            .assertChildCount(1, 0, 0)
    }

    def "HTML report contains output at task level only"() {
        given:
        buildFile << passingTask("passing", true)

        when:
        succeeds("passing")

        then:
        def results = aggregateResults()
        results
            .testPath(":passing suite")
            .assertStdout(equalTo(""))
            .assertStderr(equalTo(""))
        results
            .testPath(":passing suite:passing test")
            .assertStdout(equalTo("standard out text"))
            .assertStderr(equalTo("standard error text"))
    }

    def "HTML report contains failure message"() {
        given:
        buildFile << failingTask("failing")

        when:
        fails("failing")

        then:
        def results = aggregateResults()
        results
            .testPath(":failing suite:failing test")
            .assertHasResult(TestResult.ResultType.FAILURE)
            .assertFailureMessages(containsText("failure message"))
    }

    def "emits test results in error exception message when test fails"() {
        given:
        buildFile << failingTask("failing")

        when:
        fails("failing")

        then:
        failure.assertHasCause("Test(s) failed.")
        failure.assertHasErrorOutput("See the test results for more details: " + resultsUrlFor("failing"))
        resultsFor("failing")
            .testPath(":failing suite")
            .assertChildCount(1, 1, 0)

        // Aggregate results are still emitted even if we don't print the URL to console
        outputDoesNotContain("Aggregate test results")
        aggregateResults()
            .testPath(":failing suite")
            .assertChildCount(1, 1, 0)
    }

    def "does not emit aggregate test results if only one test task fails"() {
        given:
        buildFile << passingTask("passing")
        buildFile << failingTask("failing")

        when:
        fails("passing", "failing", "--continue")

        then:
        failure.assertHasErrorOutput("See the test results for more details: " + resultsUrlFor("failing"))

        // Aggregate results are still emitted even if we don't print the URL to console
        outputDoesNotContain("Aggregate test results")
        def aggregateResults = aggregateResults()
        aggregateResults.testPath(":passing suite")
            .assertChildCount(1, 0, 0)
        aggregateResults.testPath(":failing suite")
            .assertChildCount(1, 1, 0)
    }

    def "emits aggregate test results if multiple test tasks fail"() {
        given:
        buildFile << failingTask("failing1")
        buildFile << failingTask("failing2")

        when:
        fails("failing1", "failing2", "--continue")

        then:
        failure.assertHasDescription("Execution failed for task ':failing1'.")
        failure.assertHasErrorOutput("See the test results for more details: " + resultsUrlFor("failing1"))
        resultsFor("failing1")
            .testPath("failing1 suite")
            .assertChildCount(1, 1, 0)

        failure.assertHasDescription("Execution failed for task ':failing2'.")
        failure.assertHasErrorOutput("See the test results for more details: " + resultsUrlFor("failing2"))
        resultsFor("failing2")
            .testPath("failing2 suite")
            .assertChildCount(1, 1, 0)

        def aggregateReportFile = file("build/reports/aggregate-test-results/index.html")
        def renderedUrl = new ConsoleRenderer().asClickableFileUrl(aggregateReportFile);
        outputContains("Aggregate test results: " + renderedUrl)
        def aggregateResults = aggregateResults()
        aggregateResults.testPath("failing1 suite")
            .assertChildCount(1, 1, 0)
        aggregateResults.testPath("failing2 suite")
            .assertChildCount(1, 1, 0)
    }

    private String resultsUrlFor(String name) {
        def expectedReportFile = file("build/reports/tests/${name}/index.html")
        String renderedUrl = new ConsoleRenderer().asClickableFileUrl(expectedReportFile);
        renderedUrl
    }

    private GenericTestExecutionResult resultsFor(String name) {
        return new GenericHtmlTestExecutionResult(testDirectory, "build/reports/tests/${name}")
    }

    private GenericTestExecutionResult aggregateResults() {
        return new GenericHtmlTestExecutionResult(testDirectory, "build/reports/aggregate-test-results")
    }

    def passingTask(String name, boolean print = false) {
        assert !name.toCharArray().any { it.isWhitespace() }

        """
            abstract class ${name}CustomTestTask extends DefaultTask {
                @Inject
                abstract TestEventReporterFactory getTestEventReporterFactory()

                @Inject
                abstract ProjectLayout getLayout()

                @TaskAction
                void runTests() {
                    try (def reporter = testEventReporterFactory.createTestEventReporter(
                        "${name}",
                        getLayout().getBuildDirectory().dir("test-results/${name}").get(),
                        getLayout().getBuildDirectory().dir("reports/tests/${name}").get()
                    )) {
                       reporter.started(java.time.Instant.now())
                       try (def mySuite = reporter.reportTestGroup("${name} suite")) {
                            mySuite.started(java.time.Instant.now())
                            try (def myTest = mySuite.reportTest("${name} test", "passing test")) {
                                 myTest.started(java.time.Instant.now())
                                 ${print ? 'myTest.output(java.time.Instant.now(), org.gradle.api.tasks.testing.TestOutputEvent.Destination.StdOut, "standard out text")' : ""}
                                 ${print ? 'myTest.output(java.time.Instant.now(), org.gradle.api.tasks.testing.TestOutputEvent.Destination.StdErr, "standard error text")' : ""}
                                 myTest.succeeded(java.time.Instant.now())
                            }
                            mySuite.succeeded(java.time.Instant.now())
                       }
                       reporter.succeeded(java.time.Instant.now())
                   }
                }
            }

            tasks.register("${name}", ${name}CustomTestTask)
        """
    }

    def failingTask(String name) {
        assert !name.toCharArray().any { it.isWhitespace() }

        """
            abstract class ${name}CustomTestTask extends DefaultTask {
                @Inject
                abstract TestEventReporterFactory getTestEventReporterFactory()

                @Inject
                abstract ProjectLayout getLayout()

                @TaskAction
                void runTests() {
                    try (def reporter = testEventReporterFactory.createTestEventReporter(
                        "${name}",
                        getLayout().getBuildDirectory().dir("test-results/${name}").get(),
                        getLayout().getBuildDirectory().dir("reports/tests/${name}").get()
                    )) {
                       reporter.started(java.time.Instant.now())
                       try (def mySuite = reporter.reportTestGroup("${name} suite")) {
                            mySuite.started(java.time.Instant.now())
                            try (def myTest = mySuite.reportTest("${name} test", "failing test")) {
                                 myTest.started(java.time.Instant.now())
                                 myTest.failed(java.time.Instant.now(), "failure message")
                            }
                            mySuite.failed(java.time.Instant.now())
                       }
                       reporter.failed(java.time.Instant.now())
                   }
                }
            }

            tasks.register("${name}", ${name}CustomTestTask)
        """
    }
}
