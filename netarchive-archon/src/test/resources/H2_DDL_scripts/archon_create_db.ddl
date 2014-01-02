CREATE TABLE ARCHON (      
       FILENAME VARCHAR(512) PRIMARY KEY,
       CREATED_TIME BIGINT NOT NULL,
       SHARD_ID INT,
       ARC_STATE VARCHAR(32) NOT NULL,
       PRIORITY INT NOT NULL,
       MODIFIED_TIME BIGINT NOT NULL                     
);

CREATE UNIQUE INDEX FILENAME_ID_IN ON ARCHON(FILENAME);
CREATE INDEX CREATED_TIME_IN ON ARCHON(CREATED_TIME);
CREATE INDEX SHARDID_IN ON ARCHON(SHARD_ID);
CREATE INDEX ARC_STATE_IN ON ARCHON(ARC_STATE);
CREATE INDEX PRIORITY_IN ON ARCHON(PRIORITY);
              
              
