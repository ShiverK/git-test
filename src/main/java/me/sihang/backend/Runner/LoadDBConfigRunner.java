package me.sihang.backend.Runner;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import me.sihang.backend.util.ConstantMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Order(value = 1)
@Configuration
public class LoadDBConfigRunner implements CommandLineRunner {


    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Bucket bucket;

    @Autowired
    private ConstantMap constantMap;

    @Override
    public void run(String... args) throws Exception {
        loadConstantFromDB();
    }

    private void loadConstantFromDB(){
        loadMD5ValidationFlag();
    }

    private void loadMD5ValidationFlag(){
        JsonDocument md5ValidationFlagDoc = bucket.get("MD5ValidationFlag@constant");
        if(md5ValidationFlagDoc == null){
            md5ValidationFlagDoc = constantMap.createMD5ValidationFlagDocument();
        }
        JsonObject md5ValidationFlagInfo = md5ValidationFlagDoc.content();
        constantMap.setMD5_validation_flag(md5ValidationFlagInfo.getString("MD5ValidationFlag"));
        log.info("loaded  MD5_validation_flag is ["+constantMap.getMD5_validation_flag()+"]");
    }
}
