package app.domain.cart.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.domain.cart.model.dto.AddCartItemRequest;
import app.domain.cart.service.CartMcpService;
import app.domain.cart.status.CartSuccessStatus;
import app.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mcp/cart")
public class CartMcpController {
	private final CartMcpService cartMcpService;

	@Operation(summary = "장바구니 아이템 추가 API", description = "장바구니에 메뉴 아이템을 추가합니다. 다른 매장의 메뉴 추가 시 기존 장바구니는 초기화됩니다.")
	@PostMapping()
	public ApiResponse<String> addItemToCart(@Valid @RequestBody AddCartItemRequest request,@RequestParam Long userId) {
		String result = cartMcpService.addCartItem(userId,request);
		return ApiResponse.onSuccess(CartSuccessStatus.CART_ITEM_ADDED, result);
	}

	@Operation(summary = "장바구니 전체 삭제 API", description = "사용자의 장바구니를 전체 삭제합니다.")
	@DeleteMapping()
	public ApiResponse<String> clearCart(@RequestParam Long userId) {
		String result = cartMcpService.clearCartItems(userId);
		return ApiResponse.onSuccess(CartSuccessStatus.CART_CLEARED, result);
	}

}
