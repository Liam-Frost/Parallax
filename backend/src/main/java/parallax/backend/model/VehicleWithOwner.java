package parallax.backend.model;

public class VehicleWithOwner extends Vehicle {
    private String ownerUsername;
    private String ownerEmail;
    private String ownerPhone;
    private String ownerPhoneCountry;

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getOwnerPhone() {
        return ownerPhone;
    }

    public void setOwnerPhone(String ownerPhone) {
        this.ownerPhone = ownerPhone;
    }

    public String getOwnerPhoneCountry() {
        return ownerPhoneCountry;
    }

    public void setOwnerPhoneCountry(String ownerPhoneCountry) {
        this.ownerPhoneCountry = ownerPhoneCountry;
    }
}
