package io.github.nokasegu.post_here.location.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocationRequestDto {

    private String user;
    private Double lng;
    private Double lat;


    @JsonProperty("location")
    private void unpackNested(Map<String, Object> location) {
        this.user = (String) location.get("user");
        this.lng = (Double) location.get("longitude");
        this.lat = (Double) location.get("latitude");
    }
}
