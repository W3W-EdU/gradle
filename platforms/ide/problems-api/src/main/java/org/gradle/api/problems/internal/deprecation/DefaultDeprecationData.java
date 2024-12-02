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

package org.gradle.api.problems.internal.deprecation;

import org.gradle.api.problems.deprecation.DeprecationData;
import org.gradle.api.problems.deprecation.DeprecationDataSpec;
import org.gradle.api.problems.deprecation.DeprecatedVersion;
import org.gradle.api.problems.internal.AdditionalDataBuilder;

import javax.annotation.Nullable;

public class DefaultDeprecationData implements DeprecationData {

    private final DeprecatedVersion removedIn;
    private final String replacedBy;
    private final String reason;

    public DefaultDeprecationData(@Nullable DeprecatedVersion removedIn, @Nullable String because, @Nullable String reason) {
        this.removedIn = removedIn;
        this.replacedBy = because;
        this.reason = reason;
    }

    @Override
    public DeprecatedVersion getRemovedIn() {
        return removedIn;
    }

    @Override
    public String getReplacedBy() {
        return replacedBy;
    }

    @Override
    public String getBecause() {
        return reason;
    }

    public static AdditionalDataBuilder<DeprecationData> builder(@Nullable DeprecationData from) {
        if(from == null) {
            return new DefaultDeprecationData.DefaultDeprecationDataBuilder();
        }
        return new DefaultDeprecationData.DefaultDeprecationDataBuilder(from);
    }

    private static class DefaultDeprecationDataBuilder implements DeprecationDataSpec, AdditionalDataBuilder<DeprecationData> {
        @Nullable
        private DeprecatedVersion removedIn = null;
        @Nullable
        private String replacedBy = null;
        @Nullable
        private String because = null;

        public DefaultDeprecationDataBuilder() {
        }

        public DefaultDeprecationDataBuilder(DeprecationData from) {
            this.removedIn = from.getRemovedIn();
            this.replacedBy = from.getReplacedBy();
            this.because = from.getBecause();
        }

        @Override
        public DeprecationDataSpec removedIn(DeprecatedVersion version) {
            this.removedIn = version;
            return this;
        }

        @Override
        public DeprecationDataSpec replacedBy(String reason) {
            this.replacedBy = reason;
            return this;
        }

        @Override
        public DeprecationDataSpec because(String reason) {
            this.because = reason;
            return this;
        }

        @Override
        public DeprecationData build() {
            return new DefaultDeprecationData(removedIn, replacedBy, because);
        }
    }
}
