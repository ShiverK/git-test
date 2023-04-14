package me.sihang.backend.config;


import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.google.gson.JsonObject;
import me.sihang.backend.Proxy.BucketProxyInvocationHandler;
import me.sihang.backend.util.JsonReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class CouchBaseConfig {
    //@Value("${hostname}")
    //private String hostname;

    @Value("${bucket}")
    private String bucket;

    @Value("${password}")
    private String password;

    public @Bean
    Cluster cluster() {
        CouchbaseEnvironment env = DefaultCouchbaseEnvironment.
                builder().connectTimeout(20000).build();

        JsonReader jsonReader = new JsonReader();
        Map config = jsonReader.readJsonFile();
        String hostname = config.get("hostname").toString();
        // System.out.println(hostname+password+this.bucket);
        return CouchbaseCluster.create(env, hostname).authenticate("fetal_sys",password);
    }

    public @Bean
    Bucket bucket() {
        Bucket bucket = cluster().openBucket(this.bucket);
        BucketProxyInvocationHandler pih = new BucketProxyInvocationHandler();
        Bucket bucketProxy = (Bucket) pih.getBucketProxy(bucket);
        return bucketProxy;
//        return cluster().openBucket(this.bucket, password);
    }

//    public @Bean
////    Bucket bucket_app() {
////        return cluster().openBucket(bucket_app, password);
////    }
}
