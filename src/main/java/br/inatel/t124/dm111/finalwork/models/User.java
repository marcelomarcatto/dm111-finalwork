package br.inatel.t124.dm111.finalwork.models;

import java.security.Principal;
import java.util.Date;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.br.CPF;


public class User implements Principal {

	private long id;
	
	private String gcmRegId;
	
	@Email
	private String email;
	
	@NotNull
	private String password;
	
	private Date lastLogin;
	
	private Date lastGCMRegister;
	
	@NotNull
	private String role;
	
	@NotNull
	private String cpf;
	
	private String customerId;
	
	private String customerCRMId;

	@Override
	public String getName() {
		return this.email;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getGcmRegId() {
		return gcmRegId;
	}

	public void setGcmRegId(String gcmRegId) {
		this.gcmRegId = gcmRegId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Date getLastLogin() {
		return lastLogin;
	}

	public void setLastLogin(Date lastLogin) {
		this.lastLogin = lastLogin;
	}

	public Date getLastGCMRegister() {
		return lastGCMRegister;
	}

	public void setLastGCMRegister(Date lastGCMRegister) {
		this.lastGCMRegister = lastGCMRegister;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getCpf() {
		return cpf;
	}

	public void setCpf(String cpf) {
		this.cpf = cpf;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getCustomerCRMId() {
		return customerCRMId;
	}

	public void setCustomerCRMId(String customerCRMId) {
		this.customerCRMId = customerCRMId;
	}

}
