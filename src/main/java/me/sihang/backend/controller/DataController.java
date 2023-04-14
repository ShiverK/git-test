package me.sihang.backend.controller;

import com.couchbase.client.java.document.json.JsonObject;
import com.google.gson.Gson;
import me.sihang.backend.domain.ConfigPath;
import me.sihang.backend.service.DataService;
import me.sihang.backend.service.FileStorageService;
import me.sihang.backend.util.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import lombok.extern.java.Log;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.*;
import java.text.SimpleDateFormat;

@RequestMapping("/data")
@RestController
@CrossOrigin
public class DataController {

    //@Value("${datapath}")
    //private String datapath;

    //@Value("${server_url}")
    //private String server_url;
    private final String datapath;
    private final String server_url;

    @Autowired
    private RedisUtils ru;

    @Autowired
    private DataService dataService;

    @Autowired
    public DataController(ConfigPath configPath) {
        this.datapath = configPath.getDatapath();
        this.server_url = configPath.getServer_url();
    }


    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("getDataListForCaseId")
    public String getDataListForCaseId(String caseId) {
        //File folder = new File(this.datapath + caseId);
//        caseId = "1.3.46.670589.11.33665.5.0.28108.2017010309030010000";

        String dcmPath = fileStorageService.getDcmPath(caseId);
        // debug
        System.out.println("dcmPath:"+dcmPath);
        System.out.println("caseId:"+caseId);

        URI uri = dcmPath.isEmpty() ? Paths.get(this.datapath, caseId).toUri() : Paths.get(dcmPath).toUri();

        // debug
        System.out.println("uri:"+uri);

        File folder = new File(uri);
        File[] listOfFiles = folder.listFiles();
        ArrayList<String> lst = new ArrayList<>();
        String replacedCaseId = caseId.replace("#", "%23");
        System.out.println("caseid = " + replacedCaseId);
        System.out.println("files = " + listOfFiles);
        for (int i = 0; i < listOfFiles.length; i++) {
            String fileName = listOfFiles[i].getName();
            if (fileName.endsWith("dcm")) {
                lst.add("dicomweb://" + server_url + "/data/" + replacedCaseId + "/" + fileName);
            }
        }
        Collections.sort(lst);
        return new Gson().toJson(lst);
    }

    @PostMapping("getDataListForCaseId_APP")
    public String getDataListForCaseId_APP(String caseId) {
        //File folder = new File(this.datapath + caseId);
        File folder = new File(Paths.get(this.datapath, caseId).toUri());
        File[] listOfFiles = folder.listFiles();
        ArrayList<String> lst = new ArrayList<>();
        for (int i = 0; i < listOfFiles.length; i++) {
            String fileName = listOfFiles[i].getName();
            if (fileName.endsWith("dcm")) {
                lst.add("http://" + server_url + "/data/" + caseId + "/" + fileName);
            }
        }
        Collections.sort(lst);
        return new Gson().toJson(lst);
    }


    @GetMapping(value = "{caseId}/{dicomName}")
    public ResponseEntity<Resource> loadDicomData(@PathVariable String caseId, @PathVariable String dicomName, HttpServletRequest request) {
        Resource resource = fileStorageService.loadDicomFile(caseId, dicomName);
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping(value = "zip/{filename}")
    public ResponseEntity<Resource> loadZipFile(@PathVariable String filename) {
        Resource resource = fileStorageService.loadAbsFile("/tmp/" + filename);
        String contentType = "application/zip";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @PostMapping("getCovidMaskListForCaseId")
    public String getCovidMaskListForCaseId(String caseId) {
        File folder = new File(this.datapath + caseId + "/" + "covid_mask");
        File[] listOfFiles = folder.listFiles();
        ArrayList<String> lst = new ArrayList<>();
        for (int i = 0; i < listOfFiles.length; i++) {
            String fileName = listOfFiles[i].getName();
            if (fileName.endsWith("jpg")) {
                lst.add("http://data.deepln.deepx.machineilab.org/data/covidMask/" + caseId + "/" + fileName);
            }
        }
        Collections.sort(lst);
        return new Gson().toJson(lst);
    }

    @GetMapping(value = "covidMask/{caseId}/{jpgName}")
    public ResponseEntity<Resource> loadCovidMaskData(@PathVariable String caseId, @PathVariable String jpgName, HttpServletRequest request) {
        Resource resource = fileStorageService.loadAbsFile(this.datapath + caseId + "/" + "covid_mask" + "/" + jpgName);
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping(value = "zipForCaseId/{caseId}")
    public ResponseEntity<Resource> loadZipForCaseId(@PathVariable String caseId) {
        fileStorageService.clearZipForCaseId(caseId);
        String zipPath = fileStorageService.createZipForCaseId(caseId);
        Resource resource = fileStorageService.loadAbsFile(zipPath);
        String contentType = "application/zip";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @PostMapping(value = "upload")
    public String Upload(@RequestParam("file") MultipartFile srcFil) {


        Map<String, Object> res = new HashMap<>();
        List<String> successList = new ArrayList<>();

        if (!srcFil.isEmpty()) {
            String filename = fileStorageService.storeFile(srcFil);
            successList.add(filename);
        }


        res.put("success", successList);

        return new Gson().toJson(res);
    }

    @PostMapping(value = "uploadMutiply")
    public String MultiUpload(@RequestParam("files") MultipartFile[] srcFil) {


        Map<String, Object> res = new HashMap<>();
        List<String> successList = new ArrayList<>();

        for (MultipartFile file : srcFil) {
            if (!file.isEmpty()) {
                String filename = fileStorageService.storeFile(file);
                successList.add(filename);
            }
        }


        res.put("success", successList);

        return new Gson().toJson(res);
    }

    @PostMapping(value = "uploadSegmentationFiles/{caseId}")
    public String uploadSegmentationFiles(@RequestParam("files") MultipartFile[] srcFil, @PathVariable String caseId) {


        Map<String, Object> res = new HashMap<>();
        List<String> successList = new ArrayList<>();

        for (MultipartFile file : srcFil) {
            if (!file.isEmpty()) {
                String filename = fileStorageService.storeUploadSegmentation(file, caseId);
                successList.add(filename);
            }
        }

        Map<String, Object> caseInfo = new HashMap<>();
        caseInfo.put("case_id", caseId);
        caseInfo.put("flag", "");
        ru.lSet("testmha", new Gson().toJson(caseInfo));
        if (ru.lSet("for_service_mha2vtp", new Gson().toJson(caseInfo))) {
            res.put("mh2vtp", "ok");
        } else {
            res.put("mh2vtp", "failed");
        }
        res.put("success", successList);

        return new Gson().toJson(res);
    }

    @GetMapping(value = "slicerJumpUrl")
    public String slicerJumpUrl(String caseId) {
        Map<String, Object> res = new HashMap<>();

        String dicomweb_endpoint = "http://" + server_url + "/data/getDataListForCaseId";
        String server_uri_endpoint = "http://" + server_url;
        String segmentation_endpoint = "http://" + server_url + "/data/getMhaListForCaseId";


        String jumpUrl = "slicer://viewer/?caseId=" + caseId;
        jumpUrl += "&dicomweb_endpoint=" + dicomweb_endpoint;
        jumpUrl += "&server_uri_endpoint=" + server_uri_endpoint;
        jumpUrl += "&segmentation_endpoint=" + segmentation_endpoint;


        res.put("jumpUrl", jumpUrl);
        return jumpUrl;

    }


    @PostMapping(value = "uploadDicomDir/{token}/{isLastFile}")
    public String uploadDicomDir(@RequestParam("files") MultipartFile[] srcFil, @PathVariable String token, @PathVariable int isLastFile) {

        Date date = new Date();
        SimpleDateFormat yearAndMonthDf = new SimpleDateFormat("yyyy-MM-dd");

        String yearAndMonth = yearAndMonthDf.format(date);


        Map<String, Object> res = new HashMap<>();
        List<String> errorLst = new ArrayList<>();

        //save file
        for (MultipartFile file : srcFil) {
            if (!file.isEmpty()) {
                if (!fileStorageService.storeFileToDir(yearAndMonth, token, file)) {
                    errorLst.add(StringUtils.cleanPath(file.getOriginalFilename()));
                }
            }
        }


        res.put("errorLst", errorLst);

        //add to preprocess set
        if (isLastFile > 0) {
            if (ru.sSet("data_path_single_set", this.fileStorageService.getUploadDicomDir(yearAndMonth, token)) > 0) {
                res.put("status", "ok");
            } else {
                res.put("status", "failed");
            }
        } else {
            res.put("status", "ok");
        }


        return new Gson().toJson(res);
    }

    @GetMapping(value = "lung/{filename}")
    public ResponseEntity<Resource> loadLungData(String caseId, @PathVariable String filename, HttpServletRequest request) {
        Resource resource = fileStorageService.loadDicomFile(caseId, filename);
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }


    @GetMapping(value = "vessel/{filename}")
    public ResponseEntity<Resource> loadVesselData(String caseId, @PathVariable String filename, HttpServletRequest request) {
        Resource resource = fileStorageService.loadDicomFile(caseId, filename);
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping(value = "lobe")
    public ResponseEntity<Resource> loadLobeData(String caseId, HttpServletRequest request) {
        Resource resource = fileStorageService.loadDicomFile(caseId, "segmentation_lobe.vtp");
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping(value = "lobe/{filename}")
    public ResponseEntity<Resource> loadLobeIData(String caseId, @PathVariable String filename, HttpServletRequest request) {
        Resource resource = fileStorageService.loadDicomFile(caseId, filename);
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping(value = "airway/{filename}")
    public ResponseEntity<Resource> loadAirwayData(String caseId, @PathVariable String filename, HttpServletRequest request) {
        Resource resource = fileStorageService.loadDicomFile(caseId, filename);
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping(value = "nodule/{filename}")
    public ResponseEntity<Resource> loadNoduleData(String caseId, @PathVariable String filename, HttpServletRequest request) {
        Resource resource = fileStorageService.loadDicomFile(caseId, filename);
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping(value = "segmentation/{filename}")
    public ResponseEntity<Resource> loadsegmentationData(String caseId, @PathVariable String filename, HttpServletRequest request) {
        Resource resource = fileStorageService.loadDicomFile(caseId, filename);
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

//    @PostMapping("getMhaListForCaseId")
//    public String getMhaListForCaseId(String caseId, String modelName) {
//        if (modelName == null || modelName.isEmpty()) {
//            modelName = "bm_sys";
//        }
//        String dcmPath = fileStorageService.getDcmPath(caseId);
//        URI uri = dcmPath.isEmpty() ? Paths.get(this.datapath, caseId).toUri() : Paths.get(dcmPath).toUri();
//        File folder = new File(uri);
////        File folder = new File(this.datapath + caseId);
//        File[] listOfFiles = folder.listFiles();
//
//        ArrayList<String> pathList = new ArrayList<>();
//        ArrayList<String> lunglst = new ArrayList<>();
//        ArrayList<String> lobelst = new ArrayList<>();
//        ArrayList<String> airwaylst = new ArrayList<>();
//        ArrayList<String> nodulelst = new ArrayList<>();
//        ArrayList<String> vessellst = new ArrayList<>();
//        ArrayList<String> fileNameLst = new ArrayList<>();
//
//        for (int i = 0; i < listOfFiles.length; i++) {
//            fileNameLst.add(listOfFiles[i].getName());
//        }
//        //debug
//        System.out.println("fileNameLst0: " + fileNameLst);
//
//        this.fileStorageService.filterByModified(fileNameLst);
//        //debug
//        System.out.println("fileNameLst: " + fileNameLst);
//        System.out.println("listOfFiles: " + listOfFiles);
//
//        for (int i = 0; i < fileNameLst.size(); i++) {
//            String fileName = fileNameLst.get(i);
//            if (fileName.endsWith("vtp")) {
//
//                if (fileName.equals("segmentation_lung.vtp") || fileName.equals("segmentation_lung_modified.vtp")) {
//                    lunglst.add("http://" + server_url + "/data/lung/" + fileName);
//                } else if (fileName.equals("segmentation_airway.vtp") || fileName.equals("segmentation_airway_modified.vtp")) {
//                    airwaylst.add("http://" + server_url + "/data/airway/" + fileName);
//                } else if (fileName.startsWith("segmentation_lobe_")) {
//                    lobelst.add("http://" + server_url + "/data/lobe/" + fileName);
//                }
////                else if(fileName.startsWith("segmentation_nodule_"+modelName)){
//                else if (fileName.matches("segmentation_nodule_" + modelName + "_\\d+.vtp")) {
//                    nodulelst.add("http://" + server_url + "/data/nodule/" + fileName);
//                } else if (fileName.equals("segmentation_vessel.vtp") || fileName.equals("segmentation_vessel_modified.vtp")) {
//                    vessellst.add("http://" + server_url + "/data/vessel/" + fileName);
//                } else if (fileName.equals("segmentation_pa.vtp") || fileName.equals("segmentation_pa_modified.vtp")) {
//                    vessellst.add("http://" + server_url + "/data/vessel/" + fileName);
//                } else if (fileName.equals("segmentation_pv.vtp") || fileName.equals("segmentation_pv_modified.vtp")) {
//                    vessellst.add("http://" + server_url + "/data/vessel/" + fileName);
//                }
//            }
//        }
//
//        pathList.addAll(lunglst);
//        pathList.addAll(lobelst);
//        pathList.addAll(airwaylst);
//        pathList.addAll(nodulelst);
//        pathList.addAll(vessellst);
//        return new Gson().toJson(pathList);
//    }

    @PostMapping("getMhaListForCaseId")
    public String getMhaListForCaseId(String caseId, String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            modelName = "bm_sys";
        }
        String dcmPath = fileStorageService.getDcmPath(caseId);
        URI uri = dcmPath.isEmpty() ? Paths.get(this.datapath, caseId).toUri() : Paths.get(dcmPath).toUri();
        File folder = new File(uri);
//        File folder = new File(this.datapath + caseId);
        File[] listOfFiles = folder.listFiles();

        ArrayList<String> pathList = new ArrayList<>();
        ArrayList<String> lunglst = new ArrayList<>();
        ArrayList<String> lobelst = new ArrayList<>();
        ArrayList<String> airwaylst = new ArrayList<>();
        ArrayList<String> nodulelst = new ArrayList<>();
        ArrayList<String> vessellst = new ArrayList<>();
        ArrayList<String> fileNameLst = new ArrayList<>();

        for (int i = 0; i < listOfFiles.length; i++) {
            fileNameLst.add(listOfFiles[i].getName());
        }
        //debug
//        System.out.println("uri: " + uri);


        this.fileStorageService.filterByModified(fileNameLst);
//        //debug
//        System.out.println("fileNameLst: " + fileNameLst);
//        System.out.println("listOfFiles: " + listOfFiles);

        for (int i = 0; i < fileNameLst.size(); i++) {
            String fileName = fileNameLst.get(i);
            if (fileName.endsWith("vtp")) {

                if (fileName.equals("segmentation_lung.vtp") || fileName.equals("segmentation_lung_modified.vtp")) {
                    lunglst.add("http://" + server_url + "/data/lung/" + fileName);
                } else if (fileName.equals("segmentation_airway.vtp") || fileName.equals("segmentation_airway_modified.vtp")) {
                    airwaylst.add("http://" + server_url + "/data/airway/" + fileName);
                } else if (fileName.startsWith("segmentation_lobe_")) {
                    lobelst.add("http://" + server_url + "/data/lobe/" + fileName);
                }
//                else if(fileName.startsWith("segmentation_nodule_"+modelName)){
                else if (fileName.matches("segmentation_nodule_" + modelName + "_\\d+.vtp")) {
                    nodulelst.add("http://" + server_url + "/data/nodule/" + fileName);
                } else if (fileName.equals("segmentation_vessel.vtp") || fileName.equals("segmentation_vessel_modified.vtp")) {
                    vessellst.add("http://" + server_url + "/data/vessel/" + fileName);
                } else if (fileName.equals("segmentation_pa.vtp") || fileName.equals("segmentation_pa_modified.vtp")) {
                    vessellst.add("http://" + server_url + "/data/vessel/" + fileName);
                } else if (fileName.equals("segmentation_pv.vtp") || fileName.equals("segmentation_pv_modified.vtp")) {
                    vessellst.add("http://" + server_url + "/data/vessel/" + fileName);
                } else if (fileName.equals("segmentation.vtp")) {
//                    lunglst.add("http://" + server_url + "/data/" + caseId + "/" + fileName);
                    lunglst.add("http://" + server_url + "/data/LRAD/" + fileName);
                }
            }
        }

        pathList.addAll(lunglst);
        pathList.addAll(lobelst);
        pathList.addAll(airwaylst);
        pathList.addAll(nodulelst);
        pathList.addAll(vessellst);
        return new Gson().toJson(pathList);
    }

    @GetMapping("getTokenFile/{filename}")
    public ResponseEntity<Resource> getTokenFile(@PathVariable String filename, HttpServletRequest request) {

        Resource resource = fileStorageService.loadAbsFile(this.datapath + filename);
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @PostMapping("getDiskInfo")
    public String getDiskInfo() {
        JsonObject diskInfo = fileStorageService.getDiskInfo();
        return new Gson().toJson(diskInfo.toMap());
    }

    @PostMapping("addClickCount")
    public String addClickCount() {
        boolean result = dataService.addClickCount();

        Map<String, Object> retMap = new HashMap<>();
        if (result) {
            retMap.put("status", "okay");
        } else {
            retMap.put("status", "failed");
        }
        return new Gson().toJson(retMap);
    }

    @RequestMapping("/admin/getClickCount")
    public String getClickCount() {
        JsonObject retObj = dataService.getClickCount();
        return new Gson().toJson(retObj.toMap());
    }

    @RequestMapping("/getPluginConfig")
    public String getPluginConfig() {
        JsonObject retObj = dataService.getPluginConfig();
        return new Gson().toJson(retObj.toMap());
    }

}
