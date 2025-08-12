package app.domain.order.client;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import app.global.apiPayload.ApiResponse;
import app.global.apiPayload.code.status.ErrorStatus;
import app.global.apiPayload.exception.GeneralException;

import app.domain.order.model.dto.response.MenuInfoResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InternalStoreClient {

    private final RestTemplate restTemplate;
    
    @Value("${store.service.url:http://localhost:8082}")
    private String storeServiceUrl;

    public ApiResponse<Boolean> isStoreExists(UUID storeId) {
        String url = storeServiceUrl + "/internal/store/" + storeId + "/exists";

        ResponseEntity<ApiResponse<Boolean>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<ApiResponse<Boolean>>() {}
        );
        
        return response.getBody();
    }

    public ApiResponse<Boolean> isStoreOwner(Long userId, UUID storeId) {
        String url = storeServiceUrl + "/internal/store/" + storeId + "/owner/" + userId;

        ResponseEntity<ApiResponse<Boolean>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<ApiResponse<Boolean>>() {}
        );
        return response.getBody();
    }

    public ApiResponse<List<MenuInfoResponse>> getMenuInfoList(List<UUID> menuIds) {
        String url = storeServiceUrl + "/internal/menus/batch";

        ResponseEntity<ApiResponse<List<MenuInfoResponse>>> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            new org.springframework.http.HttpEntity<>(menuIds),
            new ParameterizedTypeReference<ApiResponse<List<MenuInfoResponse>>>() {}
        );

        return response.getBody();
    }
}