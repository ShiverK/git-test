package me.sihang.backend.service;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import lombok.val;
import me.sihang.backend.util.MD5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.zeroturnaround.zip.ZipUtil;
import org.zeroturnaround.zip.commons.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class CartService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private Bucket bucket;

    @Value("${datapath}")
    private String datapath;

    @Autowired
    public CartService(Bucket bucket) {
        this.bucket = bucket;
    }

    public boolean saveList(String username, String cart) throws IOException {
        String keyBeforeHash = username + "_cart";
        String key = MD5.parse(keyBeforeHash);
        JsonObject obj = JsonObject.create();
        obj.put("username", username);
        obj.put("type", "cart");
        obj.put("cart", cart);
        //System.out.println(cart.length());
        if (cart.equals("")) {
            this.emptyList(username);
            return true;
        } else {
            JsonDocument doc = JsonDocument.create(key, obj);
            try {
                bucket.upsert(doc);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    public  ArrayList<Map<String,Object>> getList(String username) {
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select cart from `bm_sys` where type = 'cart' and username = $1", JsonArray.from(username))
        );
        if (result.allRows().size() == 0){
            return null;
        }
        ArrayList<Map<String,Object>> ret = new ArrayList<>();

        String cartStr = result.allRows().get(0).value().getString("cart");
        String[] cartList = cartStr.split(",");
        ArrayList<String> allCaseIdList = new ArrayList<>();
        for (int i = 0; i < cartList.length; i++) {
            allCaseIdList.add("'" + cartList[i] + "'");
        }
        String caseIdStrs = "[" + String.join(",", allCaseIdList) + "]";

        N1qlQueryResult caseIdInfo = bucket.query(
                N1qlQuery.simple("select caseId,date,description,patientId,seriesId from bm_sys a where a.type = 'record' and a.caseId in " + caseIdStrs)
        );
        for (N1qlQueryRow row : caseIdInfo) {
            Map<String,Object> caseInfo = new HashMap<>();
            caseInfo.put("caseId", row.value().get("caseId"));
            caseInfo.put("patientId", row.value().get("patientId"));
            caseInfo.put("date", row.value().get("date"));
            if(row.value().containsKey("description")){
                caseInfo.put("description", row.value().get("description"));
            }else {
                caseInfo.put("description", row.value().get("seriesId"));
            }
            ret.add(caseInfo);
        }

        return  ret;
    }

    public String createZip(String username) throws IOException {
        ArrayList<Map<String, Object>> caseIdList = this.getList(username);

        if (caseIdList == null ||caseIdList.isEmpty()){
            return null;
        }

        String packPath = datapath.replace("dcms", username);
        for (Map<String, Object> caseIdInfo:caseIdList
             ) {
            String caseId = (String)caseIdInfo.get("caseId");
            String caseIdPath = datapath + caseId;
            File src = new File(caseIdPath);
            File dest = new File(packPath + caseId);
            FileUtils.copyDirectory(src, dest);
        }

        String zipFileName = "/tmp/" + username + ".zip";
        ZipUtil.pack(new File(packPath), new File(zipFileName));
        return zipFileName;
    }

    public void emptyList(String username) throws IOException {
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select *, meta(bm_sys).id as docKey from `bm_sys` where type = 'cart' and username = $1", JsonArray.from(username))
        );
        if (result.allRows().size() == 0)
            return;
        String docKey = (String) result.allRows().get(0).value().get("docKey");
//        JsonObject jsonObject = result.allRows().get(0).value().getObject("deepln");
//        jsonObject.put("cart", "");
//        JsonDocument doc = JsonDocument.create(docKey, jsonObject);
        bucket.remove(docKey);
        String packPath = datapath.replace("dcms", username);
        FileUtils.deleteDirectory(new File(packPath));
        boolean suc = new File(packPath).mkdirs();
        System.out.println(suc);
    }
}
