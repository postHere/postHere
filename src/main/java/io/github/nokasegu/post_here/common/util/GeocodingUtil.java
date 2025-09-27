package io.github.nokasegu.post_here.common.util;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class GeocodingUtil {

    private static final String API_URL = "https://maps.googleapis.com/maps/api/geocode/json";
    @Value("${google.maps.api.key}")
    private String API_KEY;

    public String getAddressFromCoordinates(double longitude, double latitude) {

        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();

        URI uri = UriComponentsBuilder
                .fromUriString(API_URL)
                .queryParam("latlng", latitude + "," + longitude)
                .queryParam("key", API_KEY)
                .queryParam("language", "ko") // 결과를 한국어로 받기 위함
                .build(true)
                .toUri();

        log.info("URL {}", uri);
        String response = restTemplate.getForObject(uri, String.class);
        if (response == null) {
            log.info("Google API로부터 응답을 받지 못했습니다.");
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(response);
            String status = root.path("status").asText();

            if (!"OK".equals(status) || !root.path("results").isArray()) {
                log.info("주소를 찾을 수 없거나 API 에러 발생. Status: {}", status);
                return null;
            }

            JsonNode resultsNode = root.path("results");
            List<String> result = new ArrayList<>();
            for (JsonNode resultNode : resultsNode) {

                JsonNode addressTypes = resultNode.path("types");
                for (JsonNode type : addressTypes) {
                    if (type.asText().equals("sublocality_level_2")) {
                        String formattedAddress = resultNode.path("formatted_address").asText();
                        log.info("주소 발견: {}", formattedAddress);
                        result.add(makeResult(formattedAddress));
                    }
                }
            }
            
            result.sort(Comparator.comparingInt(String::length).reversed());
            return result.get(0);
        } catch (Exception e) {
            log.info("매핑 중 오류 발생 {}", e.getMessage());
            return null;
        }
    }

    public String makeResult(String address) {

        return address.substring(5);
    }
}
