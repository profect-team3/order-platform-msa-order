package app.domain.order.status;

import org.springframework.http.HttpStatus;

import app.commonUtil.apiPayload.code.BaseCode;
import app.commonUtil.apiPayload.code.ReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderSuccessStatus implements BaseCode {

	ORDER_STATUS_UPDATED(HttpStatus.OK, "ORDER201", "주문 상태 전이에 성공하였습니다."),
	ORDER_DETAIL_FETCHED(HttpStatus.OK, "ORDER202", "주문 상세 조회에 성공하였습니다."),
	ORDER_FETCHED(HttpStatus.OK,"ORDER203","모든 주문 내역 조회에 성공하였습니다"),
	ORDER_CREATED(HttpStatus.OK, "ORDER204", "주문 생성에 성공하였습니다."),
	MANAGER_GET_CUSTOMER_ORDER_OK(HttpStatus.OK, "ORDER205", "선택한 사용자의 주문 조회에 성공하였습니다."),
	ORDER_EXISTS(HttpStatus.OK,"ORDER206","해당 주문이 존재합니다"),
	ORDER_HISTORY_ADDED(HttpStatus.OK,"ORDER207","주문 history 추가를 성공하였습니다."),
	ORDER_STORE_INFO(HttpStatus.OK,"ORDER208","해당 매장의 주문 내역 조회를 성공하였습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	@Override
	public ReasonDTO getReason() {
		return ReasonDTO.builder()
			.message(message)
			.code(code)
			.build();
	}

	@Override
	public ReasonDTO getReasonHttpStatus() {
		return ReasonDTO.builder()
			.isSuccess(false)
			.message(message)
			.code(code)
			.httpStatus(httpStatus)
			.build();
	}
}