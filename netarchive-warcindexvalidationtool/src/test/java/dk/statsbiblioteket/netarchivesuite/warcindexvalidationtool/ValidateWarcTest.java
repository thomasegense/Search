package dk.statsbiblioteket.netarchivesuite.warcindexvalidationtool;

import java.util.HashSet;

public class ValidateWarcTest {

  public static void main(String[] args)  throws Exception{
       
    String warc="/netarkiv/0108/filedir/67238-102-20091209053237-00404-sb-prod-har-002.statsbiblioteket.dk.arc";               
    //String warc="/netarkiv/archiveit/ARCHIVEIT-7800-TEST-JOB285755-20170323022423574-00002.warc.gz";
    
    //String warc="/netarkiv/denstoredanske/denstoredanske1923.warc.gz";
    
   // String solr = "http://localhost:8983/solr/netarchivebuilder"; //Leave as null if you dont want to validate against a solr index
   // String solr = "http://localhost:8983/solr/collection1";
   String solr = "http://ariel:52300/solr/collection1";
    
    
    HashSet<Integer> httpStatusPrefix = new HashSet<Integer>();
    httpStatusPrefix.add(2); //2XX
    ValidateWarc validateWarc = new ValidateWarc(warc,solr,httpStatusPrefix);  
    validateWarc.validate();
  }

}
