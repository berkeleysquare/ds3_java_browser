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

package com.spectralogic.dsbrowser.gui.components.createfolder;

import com.spectralogic.ds3client.Ds3Client;

public class CreateFolderModel {

    private final Ds3Client client;

    private final String location;

    private final String bucketName;

    public CreateFolderModel() {
        this(null, "", "");
    }

    public CreateFolderModel(final Ds3Client client, final String location, final String bucketName) {
        this.client = client;
        this.location = location;
        this.bucketName = bucketName;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getLocation() {
        return location;
    }

    public Ds3Client getClient() {
        return client;
    }
}
