package com.parallax.backend.ocr;

public record OcrResult(boolean plateFound, String licenseNumber, Double confidence) {
}
