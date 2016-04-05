package br.inatel.t124.dm111.finalwork.models;

import java.io.Serializable;

public class Order implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private long id;
	
	private String customerId;
	
	private String customerCpf;
	
	private String justification;
	
	private String orderStatus;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getCustomerCpf() {
		return customerCpf;
	}

	public void setCustomerCpf(String customerCpf) {
		this.customerCpf = customerCpf;
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
