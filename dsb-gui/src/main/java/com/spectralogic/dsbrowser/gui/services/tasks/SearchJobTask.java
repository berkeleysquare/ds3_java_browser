package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.ds3client.commands.spectrads3.GetObjectsWithFullDetailsSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetObjectsWithFullDetailsSpectraS3Response;
import com.spectralogic.ds3client.models.Bucket;
import com.spectralogic.ds3client.models.BulkObject;
import com.spectralogic.ds3client.models.DetailedS3Object;
import com.spectralogic.ds3client.models.S3ObjectType;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.*;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SearchJobTask extends Ds3Task<List<Ds3TreeTableItem>> {
    private final Logger LOG = LoggerFactory.getLogger(SearchJobTask.class);
    private final List<Bucket> searchableBuckets;
    private final String searchText;
    private final Session session;
    private final Workers workers;
    private final Ds3Common ds3Common;

    public SearchJobTask(final List<Bucket> searchableBuckets, final String searchText, final Session session,
                         final Workers workers, final Ds3Common ds3Common) {
        this.searchableBuckets = searchableBuckets;
        this.searchText = searchText.trim();
        this.session = session;
        this.workers = workers;
        this.ds3Common = ds3Common;
    }

    @Override
    protected List<Ds3TreeTableItem> call() throws Exception {
        try {
            final List<Ds3TreeTableItem> list = new ArrayList<>();
            searchableBuckets.forEach(bucket -> {
                if (bucket.getName().contains(searchText)) {
                    printLog(StringBuilderUtil.bucketFoundMessage(searchText).toString(), LogType.INFO);
                    final Ds3TreeTableValue value = new Ds3TreeTableValue(bucket.getName(), bucket.getName(), Ds3TreeTableValue.Type.Bucket,
                            0, StringConstants.TWO_DASH, StringConstants.TWO_DASH, false, null);
                    list.add(new Ds3TreeTableItem(value.getName(), session, value, workers, ds3Common));
                } else {
                    final List<DetailedS3Object> detailedDs3Objects = getDetailedDs3Objects(bucket.getName());
                    if (!Guard.isNullOrEmpty(detailedDs3Objects)) {
                        final List<Ds3TreeTableItem> treeTableItems = buildTreeItems(detailedDs3Objects, bucket.getName());
                        if (!Guard.isNullOrEmpty(treeTableItems)) {
                            list.addAll(treeTableItems);
                            printLog(StringBuilderUtil.searchInBucketMessage(bucket.getName(), list.size()).toString(),
                                    LogType.INFO);
                        }
                    } else {
                        LOG.error("Search failed, DetailedS3Object was null");
                        printLog(StringBuilderUtil.searchFailedMessage().toString(), LogType.ERROR);
                    }

                }
            });
            return list;
        } catch (final Exception e) {
            LOG.error("Search failed", e);
            printLog(StringBuilderUtil.searchFailedMessage().toString() + e, LogType.ERROR);
            return null;
        }
    }

    /**
     * To print the logs in DSB
     *
     * @param message message to be print
     * @param logType Log Type
     */
    private void printLog(final String message, final LogType logType) {
        if (null != ds3Common.getDeepStorageBrowserPresenter()) {
            ds3Common.getDeepStorageBrowserPresenter().logText(message, logType);
        }
    }

    /**
     * To build the treeTableItem from the object list. This method considers the files only.
     *
     * @param detailedS3Objects object list with detailed information
     * @param bucketName        bucket's name
     * @return list of treeTableItem
     */
    private List<Ds3TreeTableItem> buildTreeItems(final List<DetailedS3Object> detailedS3Objects,
                                                  final String bucketName) {
        final List<Ds3TreeTableItem> list = new ArrayList<>();
        detailedS3Objects.forEach(itemObject -> {
                    if (!itemObject.getType().equals(S3ObjectType.FOLDER)) {
                        HBox physicalPlacementHBox = null;
                        //TO get the physical placement of the objects
                        if (itemObject.getBlobs() != null && !Guard.isNullOrEmpty(itemObject.getBlobs().getObjects())) {
                            final List<BulkObject> objects = itemObject.getBlobs().getObjects();
                            physicalPlacementHBox = getConfiguredHBox(objects);
                        }
                        final Ds3TreeTableValue treeTableValue = new Ds3TreeTableValue(bucketName, itemObject.getName(),
                                Ds3TreeTableValue.Type.File, itemObject.getSize(),
                                DateFormat.formatDate(itemObject.getCreationDate()), itemObject.getOwner(), true, physicalPlacementHBox);
                        list.add(new Ds3TreeTableItem(treeTableValue.getFullName(), session,
                                treeTableValue, workers, ds3Common));
                    }
                }
        );
        return list;
    }

    /**
     * To get the detailed object list from a bucket.
     *
     * @param bucketName bucketName
     * @return list of Detailed objects
     */
    private List<DetailedS3Object> getDetailedDs3Objects(final String bucketName) {
        try {
            final GetObjectsWithFullDetailsSpectraS3Request request = new GetObjectsWithFullDetailsSpectraS3Request()
                    .withBucketId(bucketName).withName(StringConstants.PERCENT + searchText + StringConstants.PERCENT)
                    .withIncludePhysicalPlacement(true);
            final GetObjectsWithFullDetailsSpectraS3Response responseFullDetails = session.getClient().getObjectsWithFullDetailsSpectraS3(request);
            return responseFullDetails.getDetailedS3ObjectListResult().getDetailedS3Objects();
        } catch (final Exception e) {
            LOG.error("Not able to fetch detailed object list", e);
            return null;
        }
    }

    /**
     * To add Physical placement icons and tooltip
     *
     * @param objects object
     * @return HBox
     */
    private HBox getConfiguredHBox(final List<BulkObject> objects) {
        final BulkObject bulkObject = objects.stream().findFirst().orElse(null);
        if (null != bulkObject) {
            return GetStorageLocations.addPlacementIconsandTooltip(bulkObject.getPhysicalPlacement(), bulkObject.getInCache());
        }
        return null;
    }

}