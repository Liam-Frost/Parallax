package com.parallax.backend.ocr;

import com.parallax.backend.common.ApiException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

@Component
public class OcrClient {
    private final RestClient restClient;
    private final OcrProperties properties;

    public OcrClient(OcrProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    public OcrResult detectPlate(byte[] imageBytes, String filename) {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("image", new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return filename == null ? "upload.bin" : filename;
                }
            }).contentType(MediaType.APPLICATION_OCTET_STREAM);

            Map response = restClient.post()
                    .uri("/v1/detect-plate")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(builder.build())
                    .retrieve()
                    .body(Map.class);

            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "OCR_UNAVAILABLE", "Image recognition failed.");
            }

            Object plateFound = response.get("plateFound");
            boolean found = plateFound instanceof Boolean && (Boolean) plateFound;
            String licenseNumber = response.get("licenseNumber") == null ? null : response.get("licenseNumber").toString();
            Double confidence = response.get("confidence") instanceof Number
                    ? ((Number) response.get("confidence")).doubleValue()
                    : null;

            return new OcrResult(found, licenseNumber, confidence);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "OCR_UNAVAILABLE", "Image recognition failed.");
        }
    }
}
