package kolbooking.datn.brand.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class BrandProfileUpdateRequest {

    @Size(max = 200)
    private String companyName;
    @JsonIgnore
    private boolean companyNamePresent;

    @Size(max = 50)
    private String taxCode;
    @JsonIgnore
    private boolean taxCodePresent;

    @Size(max = 150)
    private String industry;
    @JsonIgnore
    private boolean industryPresent;

    @Size(max = 500)
    private String logoUrl;
    @JsonIgnore
    private boolean logoUrlPresent;

    @Size(max = 300)
    private String website;
    @JsonIgnore
    private boolean websitePresent;

    @Size(max = 150)
    private String contactName;
    @JsonIgnore
    private boolean contactNamePresent;

    @Pattern(regexp = "^$|^0\\d{9,10}$", message = "invalid phone format")
    @Size(max = 11)
    private String contactPhone;
    @JsonIgnore
    private boolean contactPhonePresent;

    @Size(max = 500)
    private String address;
    @JsonIgnore
    private boolean addressPresent;

    private String bio;
    @JsonIgnore
    private boolean bioPresent;

    @Size(max = 100)
    private String country;
    @JsonIgnore
    private boolean countryPresent;

    @JsonProperty("companyName")
    public void setCompanyName(String companyName) {
        this.companyNamePresent = true;
        this.companyName = companyName;
    }

    @JsonProperty("taxCode")
    public void setTaxCode(String taxCode) {
        this.taxCodePresent = true;
        this.taxCode = taxCode;
    }

    @JsonProperty("industry")
    public void setIndustry(String industry) {
        this.industryPresent = true;
        this.industry = industry;
    }

    @JsonProperty("logoUrl")
    public void setLogoUrl(String logoUrl) {
        this.logoUrlPresent = true;
        this.logoUrl = logoUrl;
    }

    @JsonProperty("website")
    public void setWebsite(String website) {
        this.websitePresent = true;
        this.website = website;
    }

    @JsonProperty("contactName")
    public void setContactName(String contactName) {
        this.contactNamePresent = true;
        this.contactName = contactName;
    }

    @JsonProperty("contactPhone")
    public void setContactPhone(String contactPhone) {
        this.contactPhonePresent = true;
        this.contactPhone = contactPhone;
    }

    @JsonProperty("address")
    public void setAddress(String address) {
        this.addressPresent = true;
        this.address = address;
    }

    @JsonProperty("bio")
    public void setBio(String bio) {
        this.bioPresent = true;
        this.bio = bio;
    }

    @JsonProperty("country")
    public void setCountry(String country) {
        this.countryPresent = true;
        this.country = country;
    }
}
