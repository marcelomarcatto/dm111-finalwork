package br.inatel.t124.dm111.finalwork.models;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.br.CPF;

public class ProductOfInterest implements Serializable {
	
	private static final long serialVersionUID = -8280977957503510248L;
	
	private Long id;

	@CPF
	private String cpf;
	
	@NotNull
	private String customerId;
	
	@NotNull
	private String productId;
	
	@NotNull
	private float triggerPrice;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCpf() {
		return cpf;
	}

	public void setCpf(String customerCPF) {
		this.cpf = customerCPF;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public float getTriggerPrice() {
		return triggerPrice;
	}

	public void setTriggerPrice(float triggerPrice) {
		this.triggerPrice = triggerPrice;
	}

}
