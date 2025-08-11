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

    public boolean isStoreExists(UUID storeId) {
        String url = storeServiceUrl + "/internal/store/" + storeId + "/exists";

        ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);
        return Boolean.TRUE.equals(response.getBody());
    }

    public boolean isStoreOwner(Long userId, UUID storeId) {
        String url = storeServiceUrl + "/internal/store/" + storeId + "/owner/" + userId;
        
        ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);
        return Boolean.TRUE.equals(response.getBody());
    }

    public List<MenuInfoResponse> getMenuInfoList(List<UUID> menuIds) {
        String url = storeServiceUrl + "/internal/menus/batch";
        
        try {
            ResponseEntity<List<MenuInfoResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(menuIds),
                new ParameterizedTypeReference<List<MenuInfoResponse>>() {}
            );
            
            return response.getBody();
        } catch (HttpClientErrorException.BadRequest e) {
            throw new GeneralException(ErrorStatus.MENU_NOT_FOUND);
        }
    }
}