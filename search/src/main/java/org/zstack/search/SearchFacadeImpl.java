package org.zstack.search;

import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.search.SearchGlobalProperty;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.search.SearchConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.stopwatch.StopWatch;

/**
 * @ Author : yh.w
 * @ Date   : Created in 17:20 2020/11/20
 */
public class SearchFacadeImpl extends AbstractService implements SearchFacade {
    private static CLogger logger = Utils.getLogger(SearchFacadeImpl.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private CloudBus bus;

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIRefreshSearchIndexesMsg) {
            handle((APIRefreshSearchIndexesMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIRefreshSearchIndexesMsg msg) {
        thdf.syncSubmit(new SyncTask<Void>() {
            @Override
            public Void call() throws Exception {
                APIRefreshSearchIndexesReply reply = new APIRefreshSearchIndexesReply();
                refreshSearchIndexes();
                bus.reply(msg, reply);
                return null;
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }

            @Override
            public String getSyncSignature() {
                return "refresh-search-indexs";
            }

            @Override
            public int getSyncLevel() {
                return 5;
            }
        });
    }

    @Transactional(readOnly = true)
    private void createSearchIndexes() {
        try {
            if (!Platform.isVIPNode()) {
                logger.info("current managementNode is not vip node, skip refresh search indexes");
                return;
            }

            if (!SearchGlobalProperty.SearchAutoRegister) {
                logger.info("search module has been disabled, skip refresh search indexes");
                return;
            }

            StopWatch watch = Utils.getStopWatch();
            watch.start();
            logger.info("start refresh search indexes");
            FullTextEntityManager textEntityManager = getFullTextEntityManager();
            //required typesToIndexInParallel * (threadsToLoadObjects + 1) jdbc connections
            textEntityManager.createIndexer()
                    //.typesToIndexInParallel(2), default 1
                    .batchSizeToLoadObjects(SearchGlobalProperty.massIndexerBatchSizeToLoadObjects)
                    .threadsToLoadObjects(SearchGlobalProperty.massIndexerThreadsToLoadObjects)
                    .progressMonitor(new MassIndexerProgressMonitor() {
                        @Override
                        public void documentsBuilt(int number) {

                        }

                        @Override
                        public void entitiesLoaded(int size) {

                        }

                        @Override
                        public void addToTotalCount(long count) {
                            logger.debug(String.format("indexing is going to fetch %d primary keys", count));
                        }

                        @Override
                        public void indexingCompleted() {
                            logger.debug("indexing completed");
                        }

                        @Override
                        public void documentsAdded(long increment) {
                            
                        }
                    })
                    .idFetchSize(150)
                    .startAndWait();
            watch.stop();
            logger.info(String.format("refresh search indexes success, cost %d ms", watch.getLapse()));
        } catch (Throwable e) {
            logger.warn("a unhandled exception happened", e);
            Thread.currentThread().interrupt();
        }
    }

    private void refreshSearchIndexes() {
        thdf.syncSubmit(new SyncTask<Void>() {
            @Override
            public String getName() {
                return "refresh-search-indexes";
            }

            @Override
            public Void call() throws Exception {
                createSearchIndexes();
                return null;
            }

            @Override
            public String getSyncSignature() {
                return "refresh-search-indexex";
            }

            @Override
            public int getSyncLevel() {
                return 1;
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SearchConstant.SEARCH_FACADE_SERVICE_ID);
    }

    @Override
    public boolean start() {
        refreshSearchIndexes();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public FullTextEntityManager getFullTextEntityManager() {
        return Search.getFullTextEntityManager(dbf.getEntityManager());
    }
}
