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

package com.spectralogic.dsbrowser.gui.components.license;

import com.google.common.collect.ImmutableList;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableView;

import java.net.URL;
import java.util.ResourceBundle;

@Presenter
public class LicensePresenter implements Initializable {
    @FXML
    private TableView<LicenseModel> licenseTable;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        licenseTable.setItems(FXCollections.observableList(getModels()));
    }

    private ImmutableList<LicenseModel> getModels() {
        return ImmutableList.of(new LicenseModel("ds3_java_sdk", "Apache 2"));
    }
}
