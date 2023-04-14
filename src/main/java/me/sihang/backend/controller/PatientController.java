package me.sihang.backend.controller;

import me.sihang.backend.service.PatientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/patient")
@CrossOrigin
public class PatientController {
    private static final Logger logger = LoggerFactory.getLogger(NoduleController.class);
    private final PatientService patientService;

    @Autowired
    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping(value = "totalAgeDist")
    public String totalAgeDist() {
        return this.patientService.getAgeDist();
    }

    @GetMapping(value = "getPatientSexRatio")
    public String getPatientSexRatio() {
        return this.patientService.getPatientSexRatio();
    }

    @GetMapping(value = "MomAge")
    public String MomAge() {
        return this.patientService.MomAge();
    }
}
