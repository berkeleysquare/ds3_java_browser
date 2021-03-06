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

package com.spectralogic.dsbrowser.gui.components.interruptedjobwindow;

import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.injector.ModelContext;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.Main;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.util.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.TreeMap;

@Presenter
public class JobInfoPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(Main.class);

    private final LazyAlert ALERT = new LazyAlert("No network connection", Alert.AlertType.INFORMATION);

    @FXML
    private TreeTableView<JobInfoModel> jobListTreeTable;

    @FXML
    private Button cancelJobListButtons;

    @FXML
    private TreeTableColumn sizeColumn;

    @ModelContext
    private EndpointInfo endpointInfo;

    private final Workers workers;
    private final JobWorkers jobWorkers;
    private final JobInterruptionStore jobInterruptionStore;
    private final LoggingService loggingService;

    private Stage stage;

    @Inject
    public JobInfoPresenter(final Workers workers, final JobWorkers jobWorkers, final JobInterruptionStore jobInterruptionStore, final LoggingService loggingService) {
        this.workers = workers;
        this.jobWorkers = jobWorkers;
        this.jobInterruptionStore = jobInterruptionStore;
        this.loggingService = loggingService;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        initListeners();
        initTreeTableView();
    }

    private void initListeners() {
        cancelJobListButtons.setOnAction(event -> {
            if (CheckNetwork.isReachable(endpointInfo.getClient())) {
                final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter().getJobProgressView(), null);
                if (!Guard.isMapNullOrEmpty(jobIDMap)) {

                    final Alert closeconfirmationalert = new Alert(
                            Alert.AlertType.CONFIRMATION,
                            ""
                    );
                    final Button exitButton = (Button) closeconfirmationalert.getDialogPane().lookupButton(
                            ButtonType.OK
                    );
                    final Button cancelButton = (Button) closeconfirmationalert.getDialogPane().lookupButton(
                            ButtonType.CANCEL
                    );
                    exitButton.setText("Yes");
                    cancelButton.setText("No! I don't");

                    closeconfirmationalert.setHeaderText("Do you really want to cancel all interrupted jobs");
                    closeconfirmationalert.setContentText(jobIDMap.size() + " jobs will be cancelled. You can not recover them in future.");

                    final Optional<ButtonType> closeResponse = closeconfirmationalert.showAndWait();

                    if (closeResponse.get().equals(ButtonType.OK)) {
                        cancelAllInterruptedJobs(jobIDMap);
                        event.consume();
                    }

                    if (closeResponse.get().equals(ButtonType.CANCEL)) {
                        event.consume();
                    }
                }
            } else {
                ErrorUtils.dumpTheStack("Host " + endpointInfo.getClient().getConnectionDetails().getEndpoint() + " is unreachable. Please check your connection");
                ALERT.showAlert("Host " + endpointInfo.getClient().getConnectionDetails().getEndpoint() + " is unreachable. Please check your connection");
                LOG.info("Network in unreachable");
            }
        });
    }

    private void cancelAllInterruptedJobs(final Map<String, FilesAndFolderMap> jobIDMap) {

        final Task cancelJobId = new Task() {
            @Override
            protected String call() throws Exception {
                jobIDMap.entrySet().forEach(i -> {
                    Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText("Initiating job cancel for " + i.getKey(), LogType.INFO));
                    try {
                        endpointInfo.getClient().cancelJobSpectraS3(new CancelJobSpectraS3Request(i.getKey()));
                        LOG.info("Cancelled job.");
                    } catch (final IOException e) {
                        LOG.error("Unable to cancel job ", e);
                    } finally {
                        final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.removeJobID(jobInterruptionStore, i.getKey(), endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter());
                        ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, endpointInfo.getDeepStorageBrowserPresenter());
                    }
                });
                return null;
            }
        };

        workers.execute(cancelJobId);
        refresh(jobListTreeTable, jobInterruptionStore, endpointInfo);

        cancelJobId.setOnSucceeded(event -> {
            refresh(jobListTreeTable, jobInterruptionStore, endpointInfo);
            //ParseJobInterruptionMap.refreshCompleteTreeTableView(endpointInfo, workers);
            if (cancelJobId.getValue() != null) {
                LOG.info("Cancelled job {}", cancelJobId.getValue());
            } else {
                LOG.info("Cancelled to cancel job ");
            }
        });
    }

    private void initTreeTableView() {

        endpointInfo.getDeepStorageBrowserPresenter().logText("Loading interrupted jobs view", LogType.INFO);

        jobListTreeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        if (jobListTreeTable == null) {
            jobListTreeTable.setPlaceholder(new Label("You don't have any interrupted jobs"));
            Platform.exit();
        }

        jobListTreeTable.setRowFactory(view -> new TreeTableRow<>());

        final TreeTableColumn<JobInfoModel, Boolean> actionColumn = new TreeTableColumn<>("Action");
        actionColumn.setSortable(false);
        actionColumn.setPrefWidth(120);

        actionColumn.setCellValueFactory(
                p -> new SimpleBooleanProperty(p.getValue() != null));

        actionColumn.setCellFactory(
                p -> new ButtonCell(jobWorkers, workers, endpointInfo, jobInterruptionStore, JobInfoPresenter.this, loggingService));

        jobListTreeTable.getColumns().add(actionColumn);

        final TreeItem<JobInfoModel> rootTreeItem = new TreeItem<>();
        rootTreeItem.setExpanded(true);
        jobListTreeTable.setShowRoot(false);

        final Node oldPlaceHolder = jobListTreeTable.getPlaceholder();

        final ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(90, 90);
        jobListTreeTable.setPlaceholder(new StackPane(progress));

        final Task getJobIDs = new Task() {
            @Override
            protected Object call() throws Exception {
                endpointInfo.getDeepStorageBrowserPresenter().logText("Loading interrupted jobs", LogType.INFO);
                //to show jobs in reverse order
                final Map<String, FilesAndFolderMap> jobIDHashMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter().getJobProgressView(), null);
                final TreeMap<String, FilesAndFolderMap> jobIDTreeMap = new TreeMap<>(jobIDHashMap);
                final Map<String, FilesAndFolderMap> jobIDMap = jobIDTreeMap.descendingMap();
                if (jobIDMap != null) {
                    jobIDMap.entrySet().forEach(i -> {
                        final JobInfoModel jobModel = new JobInfoModel(i.getValue().getType(), i.getKey(), i.getValue().getDate(), i.getValue().getTotalJobSize(), i.getKey(), i.getValue().getType(), "Interrupted", JobInfoModel.Type.JOBID, i.getValue().getTargetLocation(), i.getValue().getBucket());
                        rootTreeItem.getChildren().add(new JobInfoListTreeTableItem(i.getKey(), jobModel, jobIDMap, endpointInfo.getDs3Common().getCurrentSessions().get(0), workers));
                    });
                }
                return rootTreeItem;
            }
        };
        progress.progressProperty().bind(getJobIDs.progressProperty());

        workers.execute(getJobIDs);

        getJobIDs.setOnSucceeded(event -> {
            jobListTreeTable.setPlaceholder(oldPlaceHolder);

            jobListTreeTable.setRoot(rootTreeItem);
            sizeColumn.setCellFactory(c -> new TreeTableCell<JobInfoModel, Number>() {

                @Override
                protected void updateItem(final Number item, final boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(FileSizeFormat.getFileSizeType(item.longValue()));
                    }
                }
            });
        });
    }

    public void refresh(final TreeTableView<JobInfoModel> treeTableView, final JobInterruptionStore jobInterruptionStore, final EndpointInfo endpointInfo) {

        if (stage == null) {
            stage = (Stage) treeTableView.getScene().getWindow();
        }
        final TreeItem<JobInfoModel> rootTreeItem = new TreeItem<>();
        rootTreeItem.setExpanded(true);
        treeTableView.setShowRoot(false);

        final ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(90, 90);
        treeTableView.setPlaceholder(new StackPane(progress));

        final Task getJobIDs = new Task() {
            @Override
            protected Object call() throws Exception {
                Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText("Loading interrupted jobs", LogType.INFO));
                final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter().getJobProgressView(), null);

                if (Guard.isMapNullOrEmpty(jobIDMap)) {
                    Platform.runLater(() -> stage.close());
                    return null;
                }

                jobIDMap.entrySet().forEach(i -> {
                    final FilesAndFolderMap fileAndFolder = i.getValue();
                    final JobInfoModel jobModel = new JobInfoModel(fileAndFolder.getType(), i.getKey(), fileAndFolder.getDate(), fileAndFolder.getTotalJobSize(), i.getKey(), fileAndFolder.getType(), "Interrupted", JobInfoModel.Type.JOBID, fileAndFolder.getTargetLocation(), fileAndFolder.getBucket());
                    rootTreeItem.getChildren().add(new JobInfoListTreeTableItem(i.getKey(), jobModel, jobIDMap, endpointInfo.getDs3Common().getCurrentSessions().get(0), workers));
                });

                return null;
            }
        };

        workers.execute(getJobIDs);

        progress.progressProperty().bind(getJobIDs.progressProperty());

        getJobIDs.setOnSucceeded(event -> {
            treeTableView.setRoot(rootTreeItem);
            treeTableView.setPlaceholder(new Label("Great! You don't have any interrupted jobs"));
        });

        getJobIDs.setOnCancelled(event -> {
            treeTableView.setRoot(rootTreeItem);
            treeTableView.setPlaceholder(new Label("Great! You don't have any interrupted jobs"));
        });

        getJobIDs.setOnFailed(event -> {
            treeTableView.setRoot(rootTreeItem);
            treeTableView.setPlaceholder(new Label("Great! You don't have any interrupted jobs"));
        });
    }
}
