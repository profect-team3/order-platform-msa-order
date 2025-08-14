package app.domain.cart.internal;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import app.domain.cart.service.CartService;
import app.domain.cart.status.CartSuccessStatus;
import app.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class InternalCartController {

	private final CartService cartService;
	private final InternalCartService internalCartService;

	@DeleteMapping("internal/order/cart/{userId}")
	public ApiResponse<String> clearOrderCartItems(@PathVariable Long userId) {
		return ApiResponse.onSuccess(CartSuccessStatus.CART_CLEARED,cartService.clearCartItems(userId));
	}

	@PostMapping("internal/cart/{userId}")
	public ApiResponse<String> createCart(@PathVariable Long userId){

		return ApiResponse.onSuccess(CartSuccessStatus.CART_CREATED,internalCartService.createCart(userId));
	}
}
