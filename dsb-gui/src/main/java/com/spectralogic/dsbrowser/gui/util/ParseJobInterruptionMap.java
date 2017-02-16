package com.spectralogic.dsbrowser.gui.util;

import com.google.common.collect.ImmutableMap;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3GetJob;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3PutJob;
import com.spectralogic.dsbrowser.gui.services.tasks.RecoverInterruptedJob;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;


public final class ParseJobInterruptionMap {

    private final static Logger LOG = LoggerFactory.getLogger(ParseJobInterruptionMap.class);

    public static Map<String, FilesAndFolderMap> getJobIDMap(
            final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpoints,
            final String endpoint, final DeepStorageBrowserTaskProgressView<Ds3JobTask> jobWorkers,
            final UUID jobId) {
        if (!Guard.isNullOrEmpty(endpoints) && endpoints.stream().anyMatch(i -> i.containsKey(endpoint))
                && !Guard.isStringNullOrEmpty(endpoint)) {
            final Map<String, Map<String, FilesAndFolderMap>> endpointMap = endpoints.stream().filter(i -> i
                    .containsKey(endpoint)).findFirst().orElse(null);
            final Map<String, FilesAndFolderMap> jobIDMap = endpointMap.get(endpoint);
            final HashMap<String, FilesAndFolderMap> collect = jobIDMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, HashMap::new));
            final ObservableList<Ds3JobTask> tasks = jobWorkers.getTasks();
            tasks.forEach(i -> {
                UUID uuid = null;
                if (i instanceof Ds3PutJob) {
                    uuid = ((Ds3PutJob) i).getJobId();
                } else if (i instanceof Ds3GetJob) {
                    uuid = ((Ds3GetJob) i).getJobId();
                } else if (i instanceof RecoverInterruptedJob) {
                    uuid = ((RecoverInterruptedJob) i).getUuid();
                }
                if (uuid != null && uuid != jobId && collect.containsKey(uuid.toString())) {
                    collect.remove(uuid.toString());
                }
            });
            return collect;
        } else {
            LOG.info("No endpoint present");
            return null;
        }
    }

    public static Map<String, FilesAndFolderMap> removeJobID(final JobInterruptionStore jobInterruptionStore,
                                                             final String uuid, final String endpoint,
                                                             final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
        final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> completeArrayList = jobInterruptionStore.getJobIdsModel().getEndpoints();
        if (!Guard.isNullOrEmpty(completeArrayList)
                && completeArrayList.stream().anyMatch(i -> i.containsKey(endpoint))) {
            try {
                removeJobIdFromFile(jobInterruptionStore, uuid, endpoint);
            } catch (final Exception e) {
                LOG.error("Encountered an exception when trying to remove a job", e);
                if (deepStorageBrowserPresenter != null) {
                    deepStorageBrowserPresenter.logText("Failed to remove job id " + e, LogType.ERROR);
                }
            }
        }
        if (deepStorageBrowserPresenter != null) {
            return getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpoint, deepStorageBrowserPresenter.getJobProgressView(), null);
        }
        return null;
    }

    public static void removeJobIdFromFile(final JobInterruptionStore jobInterruptionStore,
                                           final String uuid, final String endpoint) throws Exception {
        final Map<String, FilesAndFolderMap> jobIdMap;
        final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> completeArrayList = jobInterruptionStore.getJobIdsModel().getEndpoints();
        final Map<String, Map<String, FilesAndFolderMap>> endpointsImmutableMap = completeArrayList.stream().filter(i
                -> i.containsKey(endpoint)).findFirst().orElse(null);
        if (!Guard.isMapNullOrEmpty(endpointsImmutableMap)) {
            final HashMap<String, Map<String, FilesAndFolderMap>> endpointHashMap = endpointsImmutableMap.entrySet()
                    .stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, HashMap::new));
            completeArrayList.remove(endpointsImmutableMap);
            if (endpointHashMap.containsKey(endpoint)) {
                jobIdMap = endpointHashMap.get(endpoint)
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, HashMap::new));
                if (jobIdMap.containsKey(uuid)) {
                    jobIdMap.remove(uuid);
                }
                endpointHashMap.put(endpoint, jobIdMap);
            }
            completeArrayList.add(endpointHashMap);
            jobInterruptionStore.getJobIdsModel().setEndpoints(completeArrayList);
        } else {
            LOG.info("No endpoint present");
        }
        JobInterruptionStore.saveJobInterruptionStore(jobInterruptionStore);
    }

    public static void setButtonAndCountNumber(final Map<String, FilesAndFolderMap> jobIDMap,
                                               final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
        if (!Guard.isMapNullOrEmpty(jobIDMap)) {
            setInterruptJobStatus(deepStorageBrowserPresenter, true, jobIDMap.size());
        } else {
            setInterruptJobStatus(deepStorageBrowserPresenter, false, 0);
        }
    }

    private static void setInterruptJobStatus(final DeepStorageBrowserPresenter deepStorageBrowserPresenter,
                                              final boolean isJobInterrupted, final int size) {
        Platform.runLater(() -> {
            try {
                if (isJobInterrupted) {
                    deepStorageBrowserPresenter.getCircle().setVisible(true);
                    deepStorageBrowserPresenter.getJobButton().setDisable(false);
                    deepStorageBrowserPresenter.getLblCount().setText(String.valueOf(size));
                } else {
                    deepStorageBrowserPresenter.getCircle().setVisible(false);
                    deepStorageBrowserPresenter.getJobButton().setDisable(true);
                    deepStorageBrowserPresenter.getLblCount().setText("");
                }
            } catch (final Exception e) {
                LOG.error("not able to set Job status", e);
            }
        });
    }

    public static void saveValuesToFiles(final JobInterruptionStore jobInterruptionStore,
                                         final ImmutableMap<String, Path> filesMap,
                                         final ImmutableMap<String, Path> foldersMap,
                                         final String endpoint, final UUID jobId,
                                         final long totalJobSize, final String targetLocation,
                                         final String jobType, final String bucket) {
        if (jobInterruptionStore != null && jobInterruptionStore.getJobIdsModel() != null) {
            try {
                final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> completeArrayList = jobInterruptionStore.getJobIdsModel().getEndpoints();
                boolean isNonAdjacent = false;
                if (!Guard.isMapNullOrEmpty(filesMap) && !Guard.isMapNullOrEmpty(foldersMap)) {
                    isNonAdjacent = true;
                }
                final FilesAndFolderMap filesAndFolderMap = new FilesAndFolderMap(filesMap, foldersMap, jobType, DateFormat.formatDate(new Date()), isNonAdjacent, targetLocation, totalJobSize, bucket);
                if (!Guard.isNullOrEmpty(completeArrayList) && completeArrayList.stream().anyMatch(i -> i.containsKey(endpoint))) {
                    final Map<String, Map<String, FilesAndFolderMap>> endpointsImmutableMap = completeArrayList
                            .stream().filter(i -> i.containsKey(endpoint)).findFirst().orElse(null);
                    if (!Guard.isMapNullOrEmpty(endpointsImmutableMap) && endpointsImmutableMap.containsKey(endpoint)) {
                        final Map<String, FilesAndFolderMap> jobIdImmutableMap = endpointsImmutableMap.get(endpoint);
                        jobIdImmutableMap.put(jobId.toString(), filesAndFolderMap);
                    } else {
                        final Map<String, FilesAndFolderMap> jobIdHashMap = new HashMap<>();
                        jobIdHashMap.put(jobId.toString(), filesAndFolderMap);
                        endpointsImmutableMap.put(endpoint, jobIdHashMap);
                    }
                    jobInterruptionStore.getJobIdsModel().setEndpoints(completeArrayList);
                } else if (null != completeArrayList) {
                    final Map<String, Map<String, FilesAndFolderMap>> endpointsHashMap = new HashMap<>();
                    final Map<String, FilesAndFolderMap> jobIdHashMap = new HashMap<>();
                    jobIdHashMap.put(jobId.toString(), filesAndFolderMap);
                    endpointsHashMap.put(endpoint, jobIdHashMap);
                    completeArrayList.add(endpointsHashMap);
                    jobInterruptionStore.getJobIdsModel().setEndpoints(completeArrayList);
                } else {
                    LOG.info("No entry found");
                }
                JobInterruptionStore.saveJobInterruptionStore(jobInterruptionStore);
            } catch (final Exception e) {
                LOG.error("Failed to save job ids", e);
            }
        }
    }
}
