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

package org.gradle.internal.component.resolution.failure.type;

import org.gradle.api.internal.artifacts.ProjectPathClarifyingDescriber;

/**
 * An abstract {@link ResolutionFailure} that represents the situation when a requested variant has attributes
 * that are not compatible with any of the available variants.
 */
public abstract class AbstractVariantSelectionFailure implements ResolutionFailure {
    private final String requestedName;

    public AbstractVariantSelectionFailure(String requestedName) {
        this.requestedName = requestedName;
    }

    @Override
    public String getRequestedName() {
        return ProjectPathClarifyingDescriber.describe(requestedName);
    }
}
