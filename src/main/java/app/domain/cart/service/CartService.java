package app.domain.cart.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.commonUtil.apiPayload.code.status.ErrorStatus;
import app.commonUtil.apiPayload.exception.GeneralException;
import app.commonUtil.security.TokenPrincipalParser;
import app.domain.cart.model.dto.AddCartItemRequest;
import app.domain.cart.model.dto.RedisCartItem;
import app.domain.cart.model.entity.Cart;
import app.domain.cart.model.entity.CartItem;
import app.domain.cart.model.repository.CartItemRepository;
import app.domain.cart.model.repository.CartRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartService {

	private final CartRedisService cartRedisService;
	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final TokenPrincipalParser tokenPrincipalParser;

	public String addCartItem(Authentication authentication,AddCartItemRequest request) {
		String userIdStr = tokenPrincipalParser.getUserId(authentication);
		Long userId = Long.parseLong(userIdStr);
		List<RedisCartItem> items = getCartFromCache(authentication);

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

	public String updateCartItem(Authentication authentication,UUID menuId, int quantity) {
		String userIdStr = tokenPrincipalParser.getUserId(authentication);
		Long userId = Long.parseLong(userIdStr);
		List<RedisCartItem> items = getCartFromCache(authentication);

		items.stream()
			.filter(item -> item.getMenuId().equals(menuId))
			.findFirst()
			.ifPresent(item -> item.setQuantity(quantity));
		return cartRedisService.saveCartToRedis(userId, items);
	}

	public String removeCartItem(Authentication authentication,UUID menuId) {
		String userIdStr = tokenPrincipalParser.getUserId(authentication);
		Long userId = Long.parseLong(userIdStr);
		return cartRedisService.removeCartItem(userId, menuId);
	}

	public List<RedisCartItem> getCartFromCache(Authentication authentication) {
		String userIdStr = tokenPrincipalParser.getUserId(authentication);
		Long userId = Long.parseLong(userIdStr);
		if (!cartRedisService.existsCartInRedis(userId)) {
			loadDbToRedis(userId);
		}
		return cartRedisService.getCartFromRedis(userId);
	}

	public String clearCartItems(Authentication authentication) {
		String userIdStr = tokenPrincipalParser.getUserId(authentication);
		Long userId = Long.parseLong(userIdStr);
		return cartRedisService.clearCartItems(userId);
	}

	@Transactional
	public String loadDbToRedis(Long userId) {
		Cart cart = cartRepository.findByUserId(userId)
			.orElseGet(() -> cartRepository.save(Cart.builder().userId(userId).build()));

		List<CartItem> cartItems = cartItemRepository.findByCart_CartId(cart.getCartId());
		List<RedisCartItem> redisItems = cartItems.stream()
			.map(item -> RedisCartItem.builder()
				.menuId(item.getMenuId())
				.storeId(item.getStoreId())
				.quantity(item.getQuantity())
				.build())
			.toList();
		cartRedisService.saveCartToRedis(userId, redisItems);
		return "사용자 " +userId + "의 장바구니가 DB에서 Redis로 성공적으로 로드되었습니다.";
	}

	@Transactional
	public String syncRedisToDb(Long userId) {
		List<RedisCartItem> redisItems = cartRedisService.getCartFromRedis(userId);

		Cart cart = cartRepository.findByUserId(userId)
			.orElseThrow(() -> new GeneralException(ErrorStatus.CART_NOT_FOUND));

		cartItemRepository.deleteByCart_CartId(cart.getCartId());
		if (!redisItems.isEmpty()) {
			List<CartItem> cartItems = redisItems.stream()
				.map(item -> {
					return CartItem.builder()
						.cart(cart)
						.menuId(item.getMenuId())
						.storeId(item.getStoreId())
						.quantity(item.getQuantity())
						.build();
				})
				.toList();

			cartItemRepository.saveAll(cartItems);
		}
		return "사용자 " + userId + "의 장바구니가 Redis에서 DB로 성공적으로 동기화되었습니다.";
	}

	@Scheduled(initialDelay = 900000, fixedRate = 900000)
	@Transactional
	public String syncAllCartsToDb() {
		Set<String> cartKeys = cartRedisService.getAllCartKeys();
		int successCount = 0;
		for (String key : cartKeys) {
			Long userId = cartRedisService.extractUserIdFromKey(key);
			syncRedisToDb(userId);
			successCount++;
		}
		return "전체 장바구니 동기화 완료 - 성공: " + successCount + "/" + cartKeys.size();
	}
}
