package com.parallax.backend.vehicle;

import com.parallax.backend.ocr.OcrClient;
import com.parallax.backend.ocr.OcrResult;
import com.parallax.backend.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleQueryController {
    private final OcrClient ocrClient;
    private final VehicleRepository vehicleRepository;

    public VehicleQueryController(OcrClient ocrClient, VehicleRepository vehicleRepository) {
        this.ocrClient = ocrClient;
        this.vehicleRepository = vehicleRepository;
    }

    @PostMapping("/query-image")
    public ResponseEntity<VehicleImageQueryResponse> queryImage(@RequestParam("image") MultipartFile image) throws Exception {
        if (image == null || image.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGE_REQUIRED", "Image file is required");
        }

        OcrResult result = ocrClient.detectPlate(image.getBytes(), image.getOriginalFilename());
        if (!result.plateFound()) {
            return ResponseEntity.ok(new VehicleImageQueryResponse(true, false, null, false, false, result.confidence(),
                    "No readable license plate was found in the image."));
        }

        String plate = result.licenseNumber() == null ? null : result.licenseNumber().trim().toUpperCase();
        Optional<VehicleEntity> match = plate == null
                ? Optional.empty()
                : vehicleRepository.findByLicenseNumberIgnoreCase(plate);
        boolean foundInSystem = match.isPresent();
        boolean blacklisted = match.map(VehicleEntity::isBlacklisted).orElse(false);

        return ResponseEntity.ok(new VehicleImageQueryResponse(
                true,
                true,
                plate,
                foundInSystem,
                blacklisted,
                result.confidence(),
                null
        ));
    }
}
