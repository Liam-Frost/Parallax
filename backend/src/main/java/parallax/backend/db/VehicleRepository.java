package parallax.backend.db;

import parallax.backend.model.Vehicle;
import parallax.backend.model.VehicleWithOwner;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository {
    List<Vehicle> reassignVehicles(String oldUsername, String newUsername);

    List<Vehicle> findByUsername(String username);

    Optional<Vehicle> findByUsernameAndLicense(String username, String licenseNumber);

    Optional<Vehicle> findByLicense(String licenseNumber);

    Optional<Vehicle> findByPlate(String licenseNumber);

    void addVehicle(Vehicle vehicle);

    void removeVehicle(String username, String licenseNumber);

    void removeVehiclesForUser(String username);

    boolean removeByLicense(String licenseNumber);

    Optional<Vehicle> updateBlacklistStatus(String licenseNumber, boolean blacklisted);

    List<VehicleWithOwner> findAllWithOwners(UserRepository userRepository);
}
