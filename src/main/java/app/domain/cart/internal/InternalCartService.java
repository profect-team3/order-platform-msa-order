package app.domain.cart.internal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.domain.cart.model.entity.Cart;
import app.domain.cart.model.repository.CartRepository;
import app.global.apiPayload.code.status.ErrorStatus;
import app.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InternalCartService {

	private final CartRepository cartRepository;

	@Transactional
	public String createCart(Long userId) {
		try {
			Cart cart = Cart.builder()
				.userId(userId)
				.build();
			cartRepository.save(cart);
			return "success";
		} catch (Exception e) {
			throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
		}
	}
}
