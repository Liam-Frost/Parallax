package com.parallax.backend.vehicle;

import com.parallax.backend.security.SecurityUtils;
import com.parallax.backend.user.UserEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listVehicles() {
        List<VehicleEntity> vehicles = vehicleService.listVehicles();
        boolean admin = SecurityUtils.hasRole("ADMIN");
        List<VehicleResponse> responses = vehicles.stream()
                .map(vehicle -> {
                    VehicleOwnerView owner = null;
                    if (admin) {
                        UserEntity user = vehicle.getOwner();
                        owner = vehicleService.buildOwner(user);
                    }
                    return VehicleResponse.from(vehicle, owner);
                })
                .toList();
        return ResponseEntity.ok(Map.of("vehicles", responses));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> addVehicle(@RequestBody VehicleCreateRequest request) {
        vehicleService.addVehicle(request);
        return ResponseEntity.status(201).body(Map.of("success", true));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deleteVehicle(@RequestBody VehicleDeleteRequest request) {
        vehicleService.deleteVehicle(request);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/blacklist")
    public ResponseEntity<Map<String, Object>> updateBlacklist(@RequestBody BlacklistRequest request) {
        vehicleService.updateBlacklist(request);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/query")
    public ResponseEntity<VehicleQueryResponse> query(@RequestParam("license") String license) {
        return ResponseEntity.ok(vehicleService.queryLicense(license));
    }
}
