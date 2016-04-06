package br.inatel.t124.dm111.finalwork.models;

import java.io.Serializable;

public class ProductOfInterest implements Serializable {
	
	private static final long serialVersionUID = -8280977957503510248L;

	private String customerCPF;
	
	private String customerId;
	
	private String productId;
	
	private float triggerPrice;

	public String getCustomerCPF() {
		return customerCPF;
	}

	public void setCustomerCPF(String customerCPF) {
		this.customerCPF = customerCPF;
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
