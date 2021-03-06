/*
 * ****************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 *  ****************************************************************************
 */

package com.spectralogic.dsbrowser.gui.components.metadata;

import com.spectralogic.ds3client.networking.Metadata;

public class Ds3Metadata {

    private final Metadata metadata;
    private final long size;
    private final String name;

    public Ds3Metadata() {
        this(null, 0, "");
    }

    public Ds3Metadata(final Metadata metadata, final long size, final String name) {
        this.metadata = metadata;
        this.size = size;
        this.name = name;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public long getSize() {
        return size;
    }

    public String getName() {
        return name;
    }
}
