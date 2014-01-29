package dk.statsbiblioteket.netarchivesuite.arctika.builder;

import java.io.IOException;
import java.util.concurrent.*;

import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.netarchivesuite.arctika.builder.IndexWorker.RUN_STATUS;
import dk.statsbiblioteket.netarchivesuite.arctika.solr.ArctikaSolrJClient;
import dk.statsbiblioteket.netarchivesuite.arctika.solr.SolrCoreStatus;
import dk.statsbiblioteket.netarchivesuite.core.ArchonConnector;
import dk.statsbiblioteket.netarchivesuite.core.ArchonConnectorClient;

public class IndexBuilder {
    private static final Logger log = LoggerFactory.getLogger(IndexBuilderConfig.class);

    /**
     * The number of milliseconds to wait between each poll for index.isOptimized.
     */
    private static final long WAIT_OPTIMIZE = 60 * 1000L;

    /**
     * The number of milliseconds to wait between each poll for worker status..
     */
    private static final long WAIT_WORKER = 10 * 1000L;

    private final ThreadPoolExecutor executor;
    private final ExecutorCompletionService<IndexWorker> completor;
    private int activeWorkers = 0;
    private final IndexBuilderConfig config;
    private final ArctikaSolrJClient solrClient;
    private final ArchonConnectorClient archonClient;

    public static void main (String[] args) throws Exception {
        String propertyFile = System.getProperty("ArtikaPropertyFile");
        if (propertyFile == null || "".equals(propertyFile)){
            String message = "Property file location must be set. Use -DArtikaPropertyFile={path to file}";
            System.out.println(message);
            log.error(message);
            System.exit(1);
        }
        String log4JFile = System.getProperty("log4j.configuration");

        if (log4JFile  == null || "".equals(log4JFile )){
            String message = "Log4j configuration not defined, using default. Use -Dlog4j.configuration={path to file}";
            log.info(message);
            System.out.println(message);
        }

        IndexBuilderConfig config = new IndexBuilderConfig(propertyFile);
        IndexBuilder builder = new IndexBuilder(config);
        builder.buildIndex();
    }

    public IndexBuilder(IndexBuilderConfig config){
        this.config = config;
        solrClient = new ArctikaSolrJClient(config.getSolr_url());
        archonClient =  new ArchonConnectorClient(config.getArchon_url());
        executor = new ThreadPoolExecutor(
                config.getMax_concurrent_workers(), config.getMax_concurrent_workers(), 10, TimeUnit.MINUTES,
                new ArrayBlockingQueue<Runnable>(config.getMax_concurrent_workers()));
        completor = new ExecutorCompletionService<IndexWorker>(executor);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public void buildIndex() throws Exception{
        SolrCoreStatus status = solrClient.getStatus();
        log.info("Starting building index for shardID "+config.getShardId() + " with status " + status);

        //Clear shardId for old jobs that hang(status RUNNING)
        archonClient.clearIndexing(""+config.getShardId());

        //Check index has target size before we start indexing further
        if (isOptimizedLimitReached()) {
            log.info("Exiting without any updates as index size has been reached for shardID " + config.getShardId());
            return;
        }

        out: do {
            while (!isBuildLimitReached()) {
                // Start up new workers until pool is full
                while (executor.getActiveCount() < config.getMax_concurrent_workers() && startNewIndexWorker());
                if (executor.getActiveCount() == 0 && activeWorkers == 0) { // Ran out of ARCs
                    log.info("No active jobs (probably due to no more un-indexed ARCs");
                    break out; // No need to optimize when there are no more ARCs
                }
                Thread.sleep(WAIT_WORKER); //Sleep for 10 secs before checking workers
            }
        } while (!isOptimizedLimitReached());

        long indexSizeBytes = status.getIndexSizeBytes();
        float percentage= (1f*indexSizeBytes)/(1f*config.getIndex_max_sizeInBytes())*100f;
        log.info("Building of shardId="+config.getShardId()+" completed. Index limit percentage: "+percentage);
        log.info("Index status: "+status);
    }

    private boolean isBuildLimitReached() throws ExecutionException, InterruptedException, IOException, SolrServerException {
        return solrClient.getStatus().getIndexSizeBytes() >
               config.getIndex_max_sizeInBytes()*config.getOptimize_limit();
    }

    private boolean isOptimizedLimitReached() throws ExecutionException, InterruptedException, IOException, SolrServerException {
        popAll();
        long start=System.currentTimeMillis();
        if (solrClient.getStatus().isOptimized()) {
            log.debug("isOptimizedLimitReached: Index already optimized");
        } else {
            //Do the optimize
            log.info("Optimizing, size of index before optimize: "+solrClient.getStatus().getIndexSizeHumanReadable());
            solrClient.optimize();

            while (!solrClient.getStatus().isOptimized()){
                log.debug("Index not optimized yet. Waiting " + WAIT_OPTIMIZE + " ms before next check");
                Thread.sleep(WAIT_OPTIMIZE); //Sleep 60 secs between checks
            }
            SolrCoreStatus status = solrClient.getStatus();
            log.info("Optimize complete. Size of index after optimize: "+status.getIndexSizeHumanReadable()
                     + ". Optimize took "+(System.currentTimeMillis()-start) + " ms");
        }
        SolrCoreStatus status = solrClient.getStatus();
        long indexSizeBytes = status.getIndexSizeBytes();

        //Too big? Stop with error
        if (indexSizeBytes >config.getIndex_max_sizeInBytes()){
            String message = "Total screw up. Index too large. Max allowed bytes="+config.getIndex_max_sizeInBytes()
                             +" but index was: "+indexSizeBytes;
            log.error(message);
            System.err.println(message);
            throw new IllegalStateException(message);
        }

        //Big enough?
        return (indexSizeBytes > config.getIndex_target_limit()*config.getIndex_max_sizeInBytes());
    }

    private boolean startNewIndexWorker() {
        String nextARC = archonClient.nextARC(""+config.getShardId());
        if ("".equals(nextARC)){
            String message = "No more arc-files to index. Stopping index process. " +
                             "It can be continued when there are new arc-files";
            log.warn(message);
            System.out.println(message);
            return false;
        }

        IndexWorker newWorker = new IndexWorker(
                nextARC,config.getSolr_url(), config.getWorker_maxMemInMb(), config.getWorker_jar_file());
        completor.submit(newWorker);
        activeWorkers++;
        return true;
    }

    private int popAll() throws ExecutionException, InterruptedException {
        int popped = 0;
        while (activeWorkers > 0) {
            popped += popFinishedWorkers();
        }
        return popped;
    }

    private synchronized int popFinishedWorkers() throws InterruptedException, ExecutionException {
        int popped = 0;
        while (activeWorkers > executor.getActiveCount()) {
            IndexWorker worker = completor.poll(IndexWorker.WORKER_TIMEOUT, TimeUnit.MILLISECONDS).get();
            activeWorkers--;
            RUN_STATUS workerStatus = worker.getStatus();
            if (workerStatus==RUN_STATUS.COMPLETED){
                log.info("Worker completed success: " + worker.getArcFile());
                archonClient.setARCState(worker.getArcFile(), ArchonConnector.ARC_STATE.COMPLETED);

            }
            if (workerStatus==RUN_STATUS.RUN_ERROR){
                log.info("Worker FAIL: " + worker.getArcFile());
                archonClient.setARCState(worker.getArcFile(), ArchonConnector.ARC_STATE.REJECTED);
            }
            popped++;
        }
        return popped;
    }
}
