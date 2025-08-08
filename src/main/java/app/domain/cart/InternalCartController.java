package app.domain.cart;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import app.domain.cart.service.CartService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class InternalCartController {

	private final CartService cartService;

	@DeleteMapping("internal/order/cart/{userId}")
	public void isOrderExists(@PathVariable Long userId) {
		cartService.clearCartItems(userId);
	}
}
