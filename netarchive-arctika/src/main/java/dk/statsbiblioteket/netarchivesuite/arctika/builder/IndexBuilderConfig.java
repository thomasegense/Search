package dk.statsbiblioteket.netarchivesuite.arctika.builder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexBuilderConfig {

    private static final Logger log = LoggerFactory.getLogger(IndexBuilderConfig.class);

    private static long gB = 1073741824l;
    private static long mB = 1048576l; // only used for test

    private int shardId = -1;
    private String worker_jar_file;
    private int worker_maxMemInMb = -1;
    private long max_concurrent_workers = -1;
    private long index_max_sizeInGB = -1;
    private float optimize_limit = 0.96f; // 96% default value
    private float index_target_limit = 0.95f; // 95% default value
    private String archon_url;
    private String solr_url;
   
    public IndexBuilderConfig(String configFilePath) throws Exception {
        initProperties(configFilePath);
    }

    public String getWorker_jar_file() {
        return worker_jar_file;
    }

    public void setWorker_jar_file(String worker_jar_file) {
        this.worker_jar_file = worker_jar_file;
    }

    public int getWorker_maxMemInMb() {
        return worker_maxMemInMb;
    }

    public void setWorker_maxMemInMb(int worker_maxMemInMb) {
        this.worker_maxMemInMb = worker_maxMemInMb;
    }

    public long getMax_concurrent_workers() {
        return max_concurrent_workers;
    }

    public void setMax_concurrent_workers(long max_concurrent_workers) {
        this.max_concurrent_workers = max_concurrent_workers;
    }

    public long getIndex_max_sizeInGB() {
        return index_max_sizeInGB;
    }

    public void setIndex_max_sizeInGB(long index_max_sizeInGB) {
        this.index_max_sizeInGB = index_max_sizeInGB;
    }

    public long getIndex_max_sizeInBytes(){
        return mB*index_max_sizeInGB;//TODO changes        
    }
        
    public float getOptimize_limit() {
        return optimize_limit;
    }

    public void setOptimize_limit(float optimize_limit) {
        this.optimize_limit = optimize_limit;
    }

    public float getIndex_target_limit() {
        return index_target_limit;
    }

    public void setIndex_target_limit(float index_target_limit) {
        this.index_target_limit = index_target_limit;
    }

    public String getArchon_url() {
        return archon_url;
    }

    public void setArchon_url(String archon_url) {
        this.archon_url = archon_url;
    }

    public String getSolr_url() {
        return solr_url;
    }

    public void setSolr_url(String solr_url) {
        this.solr_url = solr_url;
    }

    public int getShardId() {
        return shardId;
    }

    public void setShardId(int shardId) {
        this.shardId = shardId;
    }

    private void initProperties(String configFilePath) throws Exception {

        InputStreamReader isr = new InputStreamReader(new FileInputStream(new File(configFilePath)), "ISO-8859-1");

        Properties serviceProperties = new Properties();
        serviceProperties.load(isr);
        isr.close();

        worker_maxMemInMb = Integer.parseInt(serviceProperties.getProperty("actika.worker.maxMemInMb"));
        max_concurrent_workers = Integer.parseInt(serviceProperties.getProperty("actika.max_concurrent_workers"));
        index_max_sizeInGB = Integer.parseInt(serviceProperties.getProperty("arctika.index_max_sizeInGB"));
        optimize_limit = Float.parseFloat(serviceProperties.getProperty("arctika.optimize_limit"));
        index_target_limit = Float.parseFloat(serviceProperties.getProperty("arctika.index_target_limit"));
        shardId = Integer.parseInt(serviceProperties.getProperty("arctika.shardId"));
        archon_url = serviceProperties.getProperty("arctika.archon_url");
        solr_url = serviceProperties.getProperty("arctika.solr_url");
        worker_jar_file = serviceProperties.getProperty("arctika.worker.index.jar.file");
                
        log.info("Property: worker_jar_file = " + worker_jar_file);
        log.info("Property: actika.worker.maxMemInMb = " + worker_maxMemInMb);
        log.info("Property: max_concurrent_workers = " + max_concurrent_workers);
        log.info("Property: index_max_sizeInGB = " + index_max_sizeInGB);
        log.info("Property: optimize_limit= = " + optimize_limit);
        log.info("Property: index_target_limit = " + index_target_limit);
        log.info("Property: shardId = " + shardId);
        log.info("Property: archon_url = " + archon_url);
        log.info("Property: solr_url = " + solr_url);       
    }

}
