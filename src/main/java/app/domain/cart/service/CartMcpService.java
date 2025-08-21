package app.domain.cart.service;

import java.util.List;

import org.springframework.stereotype.Service;

import app.domain.cart.model.dto.AddCartItemRequest;
import app.domain.cart.model.dto.RedisCartItem;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartMcpService {

	private final CartRedisService cartRedisService;
	private final CartService cartService;

	public String addCartItem(Long userId, AddCartItemRequest request) {

		List<RedisCartItem> items = getCartFromCache(userId);

		if (!items.isEmpty() && !items.get(0).getStoreId().equals(request.getStoreId())) {
			items.clear();
		}

		boolean isExist = items.stream().anyMatch(i -> i.getMenuId().equals(request.getMenuId()));
		if (isExist) {
			items.stream()
				.filter(item -> item.getMenuId().equals(request.getMenuId()))
				.findFirst()
				.ifPresent(item -> item.setQuantity(item.getQuantity() + request.getQuantity()));
		} else {
			items.add(RedisCartItem.builder()
				.menuId(request.getMenuId())
				.storeId(request.getStoreId())
				.quantity(request.getQuantity())
				.build());
		}

		return cartRedisService.saveCartToRedis(userId, items);
	}


	public List<RedisCartItem> getCartFromCache(Long userId) {
		if (!cartRedisService.existsCartInRedis(userId)) {
			cartService.loadDbToRedis(userId);
		}
		return cartRedisService.getCartFromRedis(userId);
	}

	public String clearCartItems(Long userId) {
		return cartRedisService.clearCartItems(userId);
	}


}
