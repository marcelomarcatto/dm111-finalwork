package br.inatel.t124.dm111.finalwork.models;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.br.CPF;

public class OrderStatus implements Serializable{

	private static final long serialVersionUID = 1L;
	
	@NotNull
	private String userId;
	
	@CPF
	private String userCpf;
	
	@NotNull
	private String justification;
	
	@NotNull
	private String orderStatus;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String customerId) {
		this.userId = customerId;
	}

	public String getUserCpf() {
		return userCpf;
	}

	public void setUserCpf(String customerCpf) {
		this.userCpf = customerCpf;
	}

	public String getJustification() {
		return justification;
	}

	public void setJustification(String justification) {
		this.justification = justification;
	}

	public String getOrderStatus() {
		return orderStatus;
	}

	public void setOrderStatus(String orderStatus) {
		this.orderStatus = orderStatus;
	}

}
