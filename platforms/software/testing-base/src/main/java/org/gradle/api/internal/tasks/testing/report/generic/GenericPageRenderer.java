/*
 * Copyright 2011 the original author or authors.
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

import com.google.common.io.Resources;
import com.google.common.net.UrlEscapers;
import org.gradle.api.internal.tasks.testing.results.serializable.SerializableTestResultStore;
import org.gradle.api.tasks.testing.TestOutputEvent;
import org.gradle.internal.html.SimpleHtmlWriter;
import org.gradle.reporting.ReportRenderer;
import org.gradle.reporting.TabbedPageRenderer;
import org.gradle.reporting.TabsRenderer;
import org.gradle.util.Path;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Map;

final class GenericPageRenderer extends TabbedPageRenderer<TestTreeModel> {
    private static final URL STYLE_URL = Resources.getResource(GenericPageRenderer.class, "style.css");

    public static String getUrlTo(Path originatingPath, Path targetPath) {
        if (originatingPath.equals(targetPath)) {
            return "#";
        }
        // We know we're emitting to the file system, so let's just use NIO Path to do the path manipulation.
        // We need the `.` for relative resolution to work properly
        java.nio.file.Path relativePath = Paths.get("./" + GenericHtmlTestReport.getFilePath(originatingPath)).getParent()
            .relativize(Paths.get("./" + GenericHtmlTestReport.getFilePath(targetPath)));
        // Escape things that aren't `/` for the URL
        StringBuilder url = new StringBuilder();
        for (java.nio.file.Path segment : relativePath) {
            url.append(UrlEscapers.urlPathSegmentEscaper().escape(segment.toString()));
            url.append('/');
        }
        // Remove trailing `/`
        return url.substring(0, url.length() - 1);
    }

    private final Map<String, SerializableTestResultStore.OutputReader> outputReaders;
    private final Map<String, String> rootDisplayNames;

    GenericPageRenderer(Map<String, SerializableTestResultStore.OutputReader> outputReaders, Map<String, String> rootDisplayNames) {
        this.outputReaders = outputReaders;
        this.rootDisplayNames = rootDisplayNames;
    }

    private void renderBreadcrumbs(SimpleHtmlWriter htmlWriter) throws IOException {
        if (getModel().getPath().equals(Path.ROOT)) {
            return;
        }
        htmlWriter.startElement("div").attribute("class", "breadcrumbs");
        for (Path path : getModel().getPath().ancestors()) {
            String title = path.equals(Path.ROOT) ? "all" : path.getName();
            htmlWriter.startElement("a").attribute("href", getUrlTo(getModel().getPath(), path)).characters(title).endElement();
            htmlWriter.characters(" > ");
        }
        htmlWriter.characters(getModel().getPath().getName());
        htmlWriter.endElement();
    }

    @Override
    protected URL getStyleUrl() {
        return STYLE_URL;
    }

    @Override
    protected String getTitle() {
        // This should maybe be the display name, but we'd need to handle different display names for the same path.
        String name = getModel().getPath().getName();
        if (name == null) {
            return "All Results";
        }
        return name;
    }

    @Override
    protected String getPageTitle() {
        return "Test results - " + getTitle();
    }

    @Override
    protected ReportRenderer<TestTreeModel, SimpleHtmlWriter> getHeaderRenderer() {
        return new ReportRenderer<TestTreeModel, SimpleHtmlWriter>() {
            @Override
            public void render(TestTreeModel model, SimpleHtmlWriter htmlWriter) throws IOException {
                renderBreadcrumbs(htmlWriter);
            }
        };
    }

    @Override
    protected ReportRenderer<TestTreeModel, SimpleHtmlWriter> getContentRenderer() {
        final TabsRenderer<TestTreeModel> tabsRenderer = new TabsRenderer<>();
        getModel().getPerRootInfo().forEach((rootName, info) -> {
            // If there is only one root, don't show the root name in the tab title, since it's just confusing.
            String tabPrefix = rootDisplayNames.size() == 1 ? "" : ("'" + rootDisplayNames.get(rootName) + "' ");
            tabsRenderer.add(tabPrefix + "summary", new PerRootTabRenderer.ForSummary(rootName));
            SerializableTestResultStore.OutputReader outputReader = outputReaders.get(rootName);
            if (outputReader.hasOutput(info.getOutputId(), TestOutputEvent.Destination.StdOut)) {
                tabsRenderer.add(tabPrefix + "standard output", new PerRootTabRenderer.ForOutput(rootName, outputReader, TestOutputEvent.Destination.StdOut));
            }
            if (outputReader.hasOutput(info.getOutputId(), TestOutputEvent.Destination.StdErr)) {
                tabsRenderer.add(tabPrefix + "error output", new PerRootTabRenderer.ForOutput(rootName, outputReader, TestOutputEvent.Destination.StdErr));
            }
        });
        return tabsRenderer;
    }
}
