/*
 * Copyright 2024 the original author or authors.
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

package org.gradle.api.internal.tasks.testing.report.generic;

import com.google.common.collect.Iterables;
import com.google.common.io.CharStreams;
import org.gradle.api.internal.tasks.testing.report.generic.MetadataRendererRegistry.MetadataRenderer;
import org.gradle.api.internal.tasks.testing.results.serializable.SerializableFailure;
import org.gradle.api.internal.tasks.testing.results.serializable.SerializableTestResult;
import org.gradle.api.internal.tasks.testing.results.serializable.SerializableTestResultStore;
import org.gradle.api.internal.tasks.testing.results.serializable.SerializedMetadata;
import org.gradle.api.tasks.testing.TestOutputEvent;
import org.gradle.api.tasks.testing.TestResult;
import org.gradle.internal.html.SimpleHtmlWriter;
import org.gradle.internal.time.TimeFormatting;
import org.gradle.reporting.ReportRenderer;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public abstract class PerRootTabRenderer extends ReportRenderer<TestTreeModel, SimpleHtmlWriter> {
    protected final String rootName;
    private TestTreeModel currentModel;

    public PerRootTabRenderer(String rootName) {
        this.rootName = rootName;
    }

    @Override
    public void render(TestTreeModel model, SimpleHtmlWriter htmlWriter) throws IOException {
        this.currentModel = model;
        TestTreeModel.PerRootInfo info = model.getPerRootInfo().get(rootName);
        render(info, htmlWriter);
    }

    protected final TestTreeModel getCurrentModel() {
        if (currentModel == null) {
            throw new IllegalStateException("No model set");
        }
        return currentModel;
    }

    protected abstract void render(TestTreeModel.PerRootInfo info, SimpleHtmlWriter htmlWriter) throws IOException;

    public static final class ForSummary extends PerRootTabRenderer {
        public ForSummary(String rootName) {
            super(rootName);
        }

        @Override
        protected void render(TestTreeModel.PerRootInfo info, SimpleHtmlWriter htmlWriter) throws IOException {
            htmlWriter.startElement("div");
            renderSummary(info, htmlWriter, info.getResult());
            if (info.getChildren().isEmpty()) {
                renderLeafDetails(info, htmlWriter);
            } else {
                renderChildren(htmlWriter);
            }
            htmlWriter.endElement();
        }

        private static void renderSummary(TestTreeModel.PerRootInfo info, SimpleHtmlWriter htmlWriter, SerializableTestResult testResult) throws IOException {
            htmlWriter.startElement("div").attribute("class", "summary");
            htmlWriter.startElement("table");
            htmlWriter.startElement("tr");

            htmlWriter.startElement("td");

            renderSummaryGroup(info, htmlWriter, testResult);

            htmlWriter.endElement();

            htmlWriter.startElement("td");
            htmlWriter.startElement("div").attribute("class", "infoBox " + getStatusClass(testResult.getResultType()) + " successRate");
            htmlWriter.startElement("div").attribute("class", "percent").characters(getFormattedSuccessRate(info)).endElement();
            htmlWriter.startElement("p").characters("successful").endElement();
            htmlWriter.endElement();
            htmlWriter.endElement();

            htmlWriter.endElement();
            htmlWriter.endElement();
            htmlWriter.endElement();
        }

        private static void renderSummaryGroup(TestTreeModel.PerRootInfo info, SimpleHtmlWriter htmlWriter, SerializableTestResult testResult) throws IOException {
            htmlWriter.startElement("div").attribute("class", "summaryGroup");
            htmlWriter.startElement("table");
            htmlWriter.startElement("tr");

            htmlWriter.startElement("td");
            htmlWriter.startElement("div").attribute("class", "infoBox");
            htmlWriter.startElement("div").attribute("class", "counter").characters(Integer.toString(info.getTotalLeafCount())).endElement();
            htmlWriter.startElement("p").characters("tests").endElement();
            htmlWriter.endElement();
            htmlWriter.endElement();

            htmlWriter.startElement("td");
            htmlWriter.startElement("div").attribute("class", "infoBox");
            htmlWriter.startElement("div").attribute("class", "counter").characters(Integer.toString(info.getFailedLeafCount())).endElement();
            htmlWriter.startElement("p").characters("failures").endElement();
            htmlWriter.endElement();
            htmlWriter.endElement();

            htmlWriter.startElement("td");
            htmlWriter.startElement("div").attribute("class", "infoBox");
            htmlWriter.startElement("div").attribute("class", "counter").characters(Integer.toString(info.getSkippedLeafCount())).endElement();
            htmlWriter.startElement("p").characters("skipped").endElement();
            htmlWriter.endElement();
            htmlWriter.endElement();

            htmlWriter.startElement("td");
            htmlWriter.startElement("div").attribute("class", "infoBox duration");
            htmlWriter.startElement("div").attribute("class", "counter").characters(getFormattedDuration(testResult)).endElement();
            htmlWriter.startElement("p").characters("duration").endElement();
            htmlWriter.endElement();
            htmlWriter.endElement();

            htmlWriter.endElement();
            htmlWriter.endElement();
            htmlWriter.endElement();
        }

        private static String getFormattedDuration(SerializableTestResult testResult) {
            return TimeFormatting.formatDurationVeryTerse(testResult.getDuration());
        }

        private static void renderLeafDetails(TestTreeModel.PerRootInfo info, SimpleHtmlWriter htmlWriter) throws IOException {
            if (info.getResult().getResultType() != TestResult.ResultType.SUCCESS && !info.getResult().getFailures().isEmpty()) {
                htmlWriter.startElement("div").attribute("class", "result-details");

                htmlWriter.startElement("h3").characters(
                    info.getResult().getResultType() == TestResult.ResultType.FAILURE ? "Failure details" : "Skip details"
                ).endElement();

                htmlWriter.startElement("span").attribute("class", "code")
                    .startElement("pre")
                    .characters("");
                for (SerializableFailure failure : info.getResult().getFailures()) {
                    htmlWriter.characters(failure.getStackTrace() + "\n");
                }
                htmlWriter.endElement()
                    .endElement();

                htmlWriter.endElement();
            }
        }

        private void renderChildren(SimpleHtmlWriter htmlWriter) throws IOException {
            htmlWriter.startElement("table");
            htmlWriter.startElement("thead");
            htmlWriter.startElement("tr");

            boolean anyNameAndDisplayNameDiffer = Iterables.any(
                getCurrentModel().getChildrenOf(rootName),
                child -> {
                    SerializableTestResult childResult = child.getPerRootInfo().get(rootName).getResult();
                    return !childResult.getName().equals(childResult.getDisplayName());
                }
            );

            htmlWriter.startElement("th").characters("Child").endElement();
            if (anyNameAndDisplayNameDiffer) {
                htmlWriter.startElement("th").characters("Name").endElement();
            }
            htmlWriter.startElement("th").characters("Tests").endElement();
            htmlWriter.startElement("th").characters("Failures").endElement();
            htmlWriter.startElement("th").characters("Skipped").endElement();
            htmlWriter.startElement("th").characters("Duration").endElement();
            htmlWriter.startElement("th").characters("Success rate").endElement();

            htmlWriter.endElement();
            htmlWriter.endElement();

            for (TestTreeModel child : getCurrentModel().getChildrenOf(rootName)) {
                TestTreeModel.PerRootInfo perRootInfo = child.getPerRootInfo().get(rootName);
                SerializableTestResult result = perRootInfo.getResult();
                String statusClass = getStatusClass(result.getResultType());
                htmlWriter.startElement("tr");
                htmlWriter.startElement("td").attribute("class", statusClass);
                htmlWriter.startElement("a")
                    .attribute("href", GenericPageRenderer.getUrlTo(getCurrentModel().getPath(), child.getPath()))
                    .characters(result.getDisplayName()).endElement();
                if (anyNameAndDisplayNameDiffer) {
                    htmlWriter.startElement("td").characters(result.getName()).endElement();
                }
                htmlWriter.endElement();
                htmlWriter.startElement("td").characters(Integer.toString(perRootInfo.getTotalLeafCount())).endElement();
                htmlWriter.startElement("td").characters(Integer.toString(perRootInfo.getFailedLeafCount())).endElement();
                htmlWriter.startElement("td").characters(Integer.toString(perRootInfo.getSkippedLeafCount())).endElement();
                htmlWriter.startElement("td").characters(getFormattedDuration(result)).endElement();
                htmlWriter.startElement("td").attribute("class", statusClass).characters(getFormattedSuccessRate(perRootInfo)).endElement();
                htmlWriter.endElement();
            }
            htmlWriter.endElement();
        }

        private static String getStatusClass(TestResult.ResultType resultType) {
            switch (resultType) {
                case SUCCESS:
                    return "success";
                case FAILURE:
                    return "failures";
                case SKIPPED:
                    return "skipped";
                default:
                    throw new IllegalStateException();
            }
        }

        private static String getFormattedSuccessRate(TestTreeModel.PerRootInfo info) {
            if (info.getTotalLeafCount() == 0) {
                return "-";
            }

            BigDecimal runTests = BigDecimal.valueOf(info.getTotalLeafCount());
            BigDecimal successful = BigDecimal.valueOf(info.getTotalLeafCount() - info.getFailedLeafCount());

            return successful.divide(runTests, 2, RoundingMode.DOWN).multiply(BigDecimal.valueOf(100)).intValue() + "%";
        }
    }

    public static final class ForOutput extends PerRootTabRenderer {
        private final SerializableTestResultStore.OutputReader outputReader;
        private final TestOutputEvent.Destination destination;

        public ForOutput(String rootName, SerializableTestResultStore.OutputReader outputReader, TestOutputEvent.Destination destination) {
            super(rootName);
            this.outputReader = outputReader;
            this.destination = destination;
        }

        @Override
        protected void render(TestTreeModel.PerRootInfo info, SimpleHtmlWriter htmlWriter) throws IOException {
            htmlWriter.startElement("span").attribute("class", "code")
                .startElement("pre")
                .characters("");
            try (Reader reader = outputReader.getOutput(info.getOutputId(), destination)) {
                CharStreams.copy(reader, htmlWriter);
            }
            htmlWriter.endElement()
                .endElement();
        }
    }

    @SuppressWarnings({"MethodMayBeStatic", "UnusedReturnValue"})
    public static final class ForMetadata extends PerRootTabRenderer {
        private final MetadataRendererRegistry metadataRendererRegistry;

        public ForMetadata(String rootName, MetadataRendererRegistry metadataRendererRegistry) {
            super(rootName);
            this.metadataRendererRegistry = metadataRendererRegistry;
        }

        @Override
        protected void render(TestTreeModel.PerRootInfo info, SimpleHtmlWriter htmlWriter) throws IOException {
            htmlWriter.startElement("div").attribute("class", "metadata");
                renderMetadataTable(info, htmlWriter);
            htmlWriter.endElement();
        }

        private void renderMetadataTable(TestTreeModel.PerRootInfo info, SimpleHtmlWriter htmlWriter) throws IOException {
            htmlWriter.startElement("table");
//                renderMetadataTableHeader(htmlWriter); // TODO: Perhaps looks better without the header on the outer table?
                renderMetadataTableBody(info.getMetadatas(), htmlWriter);
            htmlWriter.endElement();
        }

//        private SimpleHtmlWriter renderMetadataTableHeader(SimpleHtmlWriter htmlWriter) throws IOException {
//            htmlWriter.startElement("thead")
//                .startElement("tr")
//                    .startElement("th")
//                        .characters("Time")
//                    .endElement()
//                    .startElement("th")
//                        .characters("Data")
//                    .endElement()
//                .endElement()
//            .endElement();
//
//            return htmlWriter;
//        }

        private SimpleHtmlWriter renderMetadataTableBody(List<SerializedMetadata> metadatas, SimpleHtmlWriter htmlWriter) throws IOException {
            htmlWriter.startElement("tbody");
            int rowNum = 0;
                for (SerializedMetadata metadata : metadatas) {
                    htmlWriter.startElement("tr").attribute("class", rowNum++ % 2 == 0 ? "even" : "odd")
                        .startElement("td")
                            .startElement("span").attribute("class", "time")
                                .characters(formatLogTime(metadata.getLogTime())) // TODO: Perhaps better rendered as an offset from test start time?
                            .endElement()
                        .endElement()
                        .startElement("td").attribute("class", "elements");
                            renderMetadataElementsTable(metadata, htmlWriter)
                        .endElement()
                    .endElement();
                }
            htmlWriter.endElement();

            return htmlWriter;
        }

        private SimpleHtmlWriter renderMetadataElementsTable(SerializedMetadata metadata, SimpleHtmlWriter htmlWriter) throws IOException {
            htmlWriter.startElement("table");
                renderMetadataElementsTableHeader(htmlWriter);
                renderMetadataElementsTableBody(metadata.getEntries(), htmlWriter)
            .endElement();

            return htmlWriter;
        }

        private SimpleHtmlWriter renderMetadataElementsTableHeader(SimpleHtmlWriter htmlWriter) throws IOException {
            htmlWriter.startElement("thead")
                .startElement("tr")
                    .startElement("th")
                        .characters("Key")
                    .endElement()
                    .startElement("th")
                        .characters("Value")
                    .endElement()
                .endElement()
            .endElement();

            return htmlWriter;
        }

        private SimpleHtmlWriter renderMetadataElementsTableBody(List<SerializedMetadata.SerializedMetadataElement> elements, SimpleHtmlWriter htmlWriter) throws IOException {
            htmlWriter.startElement("tbody");
                for (SerializedMetadata.SerializedMetadataElement element : elements) {
                    htmlWriter.startElement("tr")
                        .startElement("td").attribute("class", "key")
                            .characters(element.getKey())
                        .endElement()
                        .startElement("td").attribute("class", "value");
                            renderMetadataElementValue(element, htmlWriter)
                        .endElement()
                    .endElement();
                }
            htmlWriter.endElement();

            return htmlWriter;
        }

        private SimpleHtmlWriter renderMetadataElementValue(SerializedMetadata.SerializedMetadataElement element, SimpleHtmlWriter htmlWriter) throws IOException {
            MetadataRenderer<?> renderer = metadataRendererRegistry.getRenderer(element.getValueType());

            try {
                return renderer.render(element.getValue(), htmlWriter);
            } catch (Exception e) {
                return (SimpleHtmlWriter) htmlWriter
                    .startElement("span").attribute("class", "unrenderable")
                        .characters("[error rendering value]")
                    .endElement();
            }
        }

        private String formatLogTime(long logTime) {
            Instant instant = Instant.ofEpochMilli(logTime);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                .withZone(ZoneId.systemDefault());

            return formatter.format(instant);
        }
    }
}
