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

package org.gradle.launcher.daemon.protocol;

import org.gradle.tooling.internal.provider.serialization.PayloadSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public class AdditionalDataReplacingObjectInputStream extends ObjectInputStream {
    private final PayloadSerializer payloadSerializer;

    public AdditionalDataReplacingObjectInputStream(InputStream inputSteam, PayloadSerializer payloadSerializer) throws IOException {
        super(inputSteam);
        this.payloadSerializer = payloadSerializer;
        enableResolveObject(true);
    }

    @Override
    protected final Object resolveObject(Object obj) {
        if (obj instanceof AdditionalDataPlaceHolder) {
            return payloadSerializer.deserialize(((AdditionalDataPlaceHolder) obj).getData());
        }
        return obj;
    }
}
