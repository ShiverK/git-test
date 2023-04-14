package me.sihang.backend.service;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import me.sihang.backend.domain.ConfigPath;
import me.sihang.backend.exception.FileStorageException;
import me.sihang.backend.exception.MyFileNotFoundException;
import me.sihang.backend.property.FileStorageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.zeroturnaround.zip.ZipUtil;
import org.zeroturnaround.zip.commons.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    @Autowired
    private Bucket bucket;

    private final String datapath;

    @Autowired
    public FileStorageService(FileStorageProperties fileStorageProperties,ConfigPath configPath) {
        this.datapath = configPath.getDatapath();
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        // Normalize file name
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // Check if the file's name contains invalid characters
            if(fileName.contains("..")) {
                throw new FileStorageException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public String storeUploadSegmentation(MultipartFile file, String caseId) {
        // Normalize file name
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        if(!fileName.endsWith("_modified.mha")){
            fileName = fileName.split("\\.")[0] + "_modified.mha";
        }
        try {
            // Check if the file's name contains invalid characters
            if(fileName.contains("..")) {
                throw new FileStorageException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            Path saveDir = Paths.get(this.datapath.toString(), caseId);
            if(!isFileExsit(saveDir.toString())){
                throw new FileStorageException("Directory doesn't exist.");
            }

            // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = Paths.get(saveDir.toString(), fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public Boolean storeFileToDir(String yearAndMonth, String dayAndTime,MultipartFile file) {
        // Normalize file name
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // Check if the file's name contains invalid characters
            if(fileName.contains("..")) {
                throw new FileStorageException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            Path saveDir = Paths.get(this.fileStorageLocation.toString(),yearAndMonth,dayAndTime);

            if(!isFileExsit(saveDir.toString())){
                try {
                    Files.createDirectories(saveDir);
                } catch (Exception ex) {
                    throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
                }
            }
            // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = Paths.get(saveDir.toString(),fileName);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public Resource loadAbsFile(String fileName) {
        try {
            Path filePath = Paths.get(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if(resource.exists()) {
                return resource;
            } else {
                throw new MyFileNotFoundException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new MyFileNotFoundException("File not found " + fileName, ex);
        }
    }

    public Resource loadDicomFile(String caseId, String filename) {
        try {
            String dcmPath = this.getDcmPath(caseId);
            Path filePath = dcmPath.isEmpty()?Paths.get(this.datapath, caseId,filename):Paths.get(dcmPath,filename);
//            String filePathStr = datapath + caseId + "/" + filename;
//            Path filePath = Paths.get(filePathStr);
            Resource resource = new UrlResource(filePath.toUri());
            if(resource.exists()) {
                return resource;
            } else {
                throw new MyFileNotFoundException("File not found " + filename);
            }
        } catch (MalformedURLException ex) {
            throw new MyFileNotFoundException("File not found " + filename, ex);
        }
    }

    public String createZipForCaseId(String caseId) {

        String originPath = datapath + caseId;

        String zipFileName = "/tmp/" + caseId + ".zip";
        ZipUtil.pack(new File(originPath), new File(zipFileName));

        return zipFileName;

    }

    public void clearZipForCaseId(String caseId) {

        String zipPath = "/tmp/" + caseId + ".zip";

        File file = new File(zipPath);

        if(file.exists()){
            boolean res = file.delete();
        }

    }

    public boolean isFileExsit(String filePath){

        File file = new File(filePath);
        return file.exists();
    }

    public String getUploadDicomDir(String yearAndMonth, String dayAndTime){
        return Paths.get(this.fileStorageLocation.toString(),yearAndMonth,dayAndTime).toString();
    }

    public JsonObject getDiskInfo(){
        JsonDocument diskInfo = bucket.get("disk@monitor");
        JsonObject returnObj = diskInfo.content();
        returnObj.removeKey("type");
        returnObj.removeKey("object");
        return returnObj;
    }

    public String getDcmPath(String caseId){
        try{
            N1qlQueryResult result = bucket.query(
                    N1qlQuery.parameterized("select dcm_path from `bm_sys` where type = 'record' and caseId = $1", JsonArray.from(caseId))
            );
            JsonObject value = (JsonObject) result.allRows().get(0).value();
            String dcmPath = value.getString("dcm_path");
            if (dcmPath == null){
                dcmPath = "";
            }
            return dcmPath;
        }catch (Exception e){
            return "";
        }
    }

    public void filterByModified(ArrayList<String> array){
        ArrayList<String> toDelete = new ArrayList<>(); 
        for (String s : array) {
            if(s.endsWith("_modified.vtp")){
                String target = s.replace("_modified", "");
                if(array.contains(target)){
                    toDelete.add(target);
                }
            }
        }
        for (String item : toDelete) {
            array.remove(item);
        }
    }
}
