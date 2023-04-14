package me.sihang.backend.controller;

import com.google.gson.Gson;

import me.sihang.backend.payload.UploadFileResponse;
import me.sihang.backend.service.FileStorageService;
import me.sihang.backend.service.RecordService;
import me.sihang.backend.service.TFService;
import me.sihang.backend.util.FileList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RequestMapping("/tf")
@RestController
public class TFController {

    private static final Logger logger = LoggerFactory.getLogger(TFController.class);

    private final FileStorageService fileStorageService;
    private final TFService tfService;

    @Autowired
    public TFController(FileStorageService fileStorageService, TFService tfService) {
        this.fileStorageService = fileStorageService;
        this.tfService = tfService;
    }

    @PostMapping("/uploadFile")
    public UploadFileResponse uploadFile(@RequestParam("file") MultipartFile file) {
        String fileName = fileStorageService.storeFile(file);

        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/downloadFile/")
                .path(fileName)
                .toUriString();

        return new UploadFileResponse(fileName, fileDownloadUri,
                file.getContentType(), file.getSize());
    }

    @PostMapping("/uploadMultipleFiles")
    public List<UploadFileResponse> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        return Arrays.asList(files)
                .stream()
                .map(file -> uploadFile(file))
                .collect(Collectors.toList());
    }

    @GetMapping(value = "download_file")
    public ResponseEntity<Resource> download(String path, HttpServletRequest request) {

        Resource resource = fileStorageService.loadAbsFile(path);

        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            logger.info("Could not determine file type.");
        }

        // Fallback to the default content type if type could not be determined
        if(contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping(value = "download_dicom_list")
    public String downloadDicomList(String patientID, String patientDate, String seriesID) {
        String caseId = tfService.getCaseIdByThreeDomains(patientID, patientDate, seriesID);
        String dataFolder = tfService.getDatapath();
        Set<String> imgList = new HashSet<>();
        try {
            imgList = FileList.fromDirectory(dataFolder + caseId);
            ArrayList<String> imgListt = new ArrayList<>(imgList);
            Collections.sort(imgListt);
            Map<String, Object> map = new HashMap<>();
            map.put("downloadlist", imgListt);
            map.put("y", "success");
            return new Gson().toJson(map);
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping(value = "download_dicom")
    public ResponseEntity<Resource> downloadDicom(String patientID, String patientDate, String seriesID, String dicomname, HttpServletRequest request) {
        String caseId = tfService.getCaseIdByThreeDomains(patientID, patientDate, seriesID);
        Resource resource = fileStorageService.loadDicomFile(caseId, dicomname);
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


    @GetMapping(value = "get_patient_list")
    public String getPatientList(String indexbegin, String indexend, String patientDate, String patientID) {
        if (patientID == null)
            patientID = "undef";
        if (patientDate == null)
            patientDate = "undef";
        return tfService.getPatientList(indexbegin, indexend, patientID, patientDate);
    }

    @PostMapping(value = "ln_getresult")
//    @RequestMapping(value = "ln_getresult", method = RequestMethod.PATCH, headers = "Accept=application/json,Content-type=application/json")
    public String lnGetResult(@RequestHeader Map<String, String> headers, @RequestBody Map<String, Object> payload, String patientID, String patientDate, String seriesID, HttpServletRequest request) throws IOException {
        String pDate;
        String pID;
        String sID;
        if (patientDate == null)
            pDate = (String) payload.get("patientDate");
        else
            pDate = patientDate;
        if (patientID == null)
            pID = (String) payload.get("patientID");
        else
            pID = patientID;
        if (seriesID == null)
            sID = (String) payload.get("seriesID");
        else
            sID = seriesID;
        String caseId = tfService.getCaseIdByThreeDomains(pID, pDate, sID);
        String ret = tfService.getRectForClient(caseId);
        return ret;
    }
}
