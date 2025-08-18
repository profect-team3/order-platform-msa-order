package app.domain.cart.internal;

import org.springframework.security.core.Authentication;
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

	@DeleteMapping("internal/order/cart")
	public ApiResponse<String> clearOrderCartItems(Authentication authentication) {
		return ApiResponse.onSuccess(CartSuccessStatus.CART_CLEARED,cartService.clearCartItems(authentication));
	}

	@PostMapping("internal/cart")
	public ApiResponse<String> createCart(Authentication authentication){

		return ApiResponse.onSuccess(CartSuccessStatus.CART_CREATED,internalCartService.createCart(authentication));
	}
}
