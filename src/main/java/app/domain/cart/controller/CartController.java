package app.domain.cart.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.commonUtil.apiPayload.ApiResponse;
import app.domain.cart.model.dto.AddCartItemRequest;
import app.domain.cart.model.dto.RedisCartItem;
import app.domain.cart.service.CartService;
import app.domain.cart.status.CartSuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "cart", description = "장바구니 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/order")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {
	private final CartService cartService;

	@Operation(summary = "장바구니 아이템 추가 API", description = "장바구니에 메뉴 아이템을 추가합니다. 다른 매장의 메뉴 추가 시 기존 장바구니는 초기화됩니다.")
	@PostMapping("/item")
	public ApiResponse<String> addItemToCart(@Valid @RequestBody AddCartItemRequest request, Authentication authentication) {
		String result = cartService.addCartItem(authentication,request);
		return ApiResponse.onSuccess(CartSuccessStatus.CART_ITEM_ADDED, result);
	}

	@Operation(summary = "장바구니 아이템 수량 수정 API", description = "장바구니에 있는 특정 메뉴의 수량을 수정합니다.")
	@PatchMapping("/item/{menuId}/{quantity}")
	public ApiResponse<String> updateItemInCart(@Valid @PathVariable UUID menuId,
		@Valid @PathVariable int quantity,Authentication authentication) {
		String result = cartService.updateCartItem(authentication,menuId, quantity);
		return ApiResponse.onSuccess(CartSuccessStatus.CART_ITEM_UPDATED, result);
	}

	@Operation(summary = "장바구니 아이템 삭제 API", description = "장바구니에서 특정 메뉴를 삭제합니다.")
	@DeleteMapping("/item/{menuId}")
	public ApiResponse<String> removeItemFromCart(@Valid @PathVariable UUID menuId,Authentication authentication) {
		String result = cartService.removeCartItem(authentication,menuId);
		return ApiResponse.onSuccess(CartSuccessStatus.CART_ITEM_REMOVED, result);
	}

	@Operation(summary = "장바구니 조회 API", description = "사용자의 장바구니 내용을 조회합니다. Redis에 없으면 DB에서 로드합니다.")
	@GetMapping("/cart")
	public ApiResponse<List<RedisCartItem>> getCart(Authentication authentication) {
		List<RedisCartItem> cartItems = cartService.getCartFromCache(authentication);
		return ApiResponse.onSuccess(CartSuccessStatus.CART_RETRIEVED, cartItems);
	}

	@Operation(summary = "장바구니 전체 삭제 API", description = "사용자의 장바구니를 전체 삭제합니다.")
	@DeleteMapping("/item")
	public ApiResponse<String> clearCart(Authentication authentication) {
		String result = cartService.clearCartItems(authentication);
		return ApiResponse.onSuccess(CartSuccessStatus.CART_CLEARED, result);
	}

}
