package app.domain.order.service;

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

import app.domain.order.model.dto.MenuInfo;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StoreServiceClient {

    private final RestTemplate restTemplate;
    
    @Value("${store.service.url:http://localhost:8083}")
    private String storeServiceUrl;

    public boolean isStoreExists(UUID storeId) {
        String url = storeServiceUrl + "/api/stores/" + storeId + "/exists";
        
        ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);
        return Boolean.TRUE.equals(response.getBody());
    }

    public boolean isStoreOwner(Long userId, UUID storeId) {
        String url = storeServiceUrl + "/api/stores/" + storeId + "/owner/" + userId;
        
        ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);
        return Boolean.TRUE.equals(response.getBody());
    }

    public List<MenuInfo> getMenuInfoList(List<UUID> menuIds) {
        String url = storeServiceUrl + "/api/menus/batch";
        
        try {
            ResponseEntity<List<MenuInfo>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(menuIds),
                new ParameterizedTypeReference<List<MenuInfo>>() {}
            );
            
            return response.getBody();
        } catch (HttpClientErrorException.BadRequest e) {
            throw new GeneralException(ErrorStatus.MENU_NOT_FOUND);
        }
    }
}