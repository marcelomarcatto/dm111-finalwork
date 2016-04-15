package br.inatel.t124.dm111.finalwork.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;

import br.inatel.t124.dm111.finalwork.models.ProductOfInterest;
import br.inatel.t124.dm111.finalwork.models.User;

@Path("/productsOfInterest")
public class ProductOfInterestManager {
	
	
	public static final String PRODUCTS_INTEREST_KIND = "ProductsOfInterest"; 
	
	public static final String PROP_CPF = "cpf";
	public static final String PROP_CUSTOMER_ID = "customerId";
	public static final String PROP_PRODUCT_ID = "productId";
	public static final String PROP_TRIGGER_PRICE = "triggerPrice";
	
	@Context
	SecurityContext securityContext;
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({"ADMIN, USER"})
	public List<ProductOfInterest> getList() {

		// TODO : validate current user

		List<ProductOfInterest> productsOfInterest = new ArrayList<>();
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		Query query = new Query(PRODUCTS_INTEREST_KIND).addSort(PROP_CPF, SortDirection.ASCENDING);
		List<Entity> poiEntities = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());

		if (poiEntities != null) {
			for (Entity poiEntity : poiEntities) {
				ProductOfInterest productOfInterest = entityToProductOfInterest(poiEntity);
				productsOfInterest.add(productOfInterest);
			}
		} else {
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		return productsOfInterest;
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@PermitAll
	public ProductOfInterest insert(@Valid ProductOfInterest productOfInterest) {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		if (getByEmail(productOfInterest.getEmail()) == null) {
			if (getByCPF(productOfInterest.getCpf()) == null) {
				if (!securityContext.isUserInRole("ADMIN")) {
					productOfInterest.setRole("USER");
				}
				Key userKey = KeyFactory.createKey(PRODUCTS_INTEREST_KIND, "userKey");
				Entity userEntity = new Entity(PRODUCTS_INTEREST_KIND, userKey);

				productOfInterest.setGcmRegId("");
				productOfInterest.setLastGCMRegister(null);
				productOfInterest.setLastLogin(null);
				productOfInterestToEntity(productOfInterest, userEntity);

				datastore.put(userEntity);
				productOfInterest.setId(userEntity.getKey().getId());
			} else {
				throw new WebApplicationException("An user with CPF " + productOfInterest.getCpf() + " already exists.",
						Status.BAD_REQUEST);
			}
		} else {
			throw new WebApplicationException("An user with email " + productOfInterest.getEmail() + " already exists.",
					Status.BAD_REQUEST);
		}
		
		return productOfInterest;
	}

	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{email}")
	@RolesAllowed({"ADMIN", "USER"})
	public User update(@PathParam("email") String email, @Valid User user) {
		if (user.getId() != 0) {

			if (securityContext.getUserPrincipal().getName().equals(email) || securityContext.isUserInRole("ADMIN")) {
				Entity userEntity = getByEmail(user.getEmail());
				if (userEntity != null) {
					if (!userEntity.getProperty(PROP_EMAIL).equals(email)) {

						Entity cpfUserEntity = getByCPF(user.getCpf());
						if (cpfUserEntity == null || cpfUserEntity.getProperty(PROP_CPF).equals(user.getCpf())) {

							DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

							productOfInterestToEntity(user, userEntity);

							if (!securityContext.isUserInRole("ADMIN")) {
								user.setRole("USER");
							}
							datastore.put(userEntity);

							return user;

						} else {
							throw new WebApplicationException("An user with CPF " + user.getCpf() + " already exists.",
									Status.BAD_REQUEST);
						}
					} else {
						throw new WebApplicationException("An user with email " + user.getEmail() + " already exists.",
								Status.BAD_REQUEST);
					}
				} else {
					throw new WebApplicationException(Status.NOT_FOUND);
					
				}
			} else {
				throw new WebApplicationException(Status.FORBIDDEN);
			}
		} else {
			throw new WebApplicationException("The user Id must be informed.", Status.BAD_REQUEST);
		}
	}
	
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{cpf}")
	@RolesAllowed({"ADMIN", "USER"})
	public User delete(@PathParam("cpf") String cpf) {
		Entity userEntity = getByCPF(cpf);
		if (userEntity != null) {
			if (securityContext.getUserPrincipal().getName().equals(userEntity.getProperty(PROP_EMAIL))
					|| securityContext.isUserInRole("ADMIN")) {
				DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
				datastore.delete(userEntity.getKey());
				User user = entityToProductOfInterest(userEntity);

				return user;
			} else {
				throw new WebApplicationException(Status.FORBIDDEN);
			}
		} else {
			throw new WebApplicationException(Status.NOT_FOUND);
		}
	}

	private Entity getByEmail(String email) {		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Filter emailFilter = new FilterPredicate(PROP_EMAIL, FilterOperator.EQUAL, email);
		
		Query query = new Query(PRODUCTS_INTEREST_KIND).setFilter(emailFilter);
		return datastore.prepare(query).asSingleEntity();
	}
	
	private Entity getByCPF(String cpf) {		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Filter emailFilter = new FilterPredicate(PROP_CPF, FilterOperator.EQUAL, cpf);
		
		Query query = new Query(PRODUCTS_INTEREST_KIND).setFilter(emailFilter);
		return datastore.prepare(query).asSingleEntity();
	}

	public static ProductOfInterest entityToProductOfInterest(Entity poiEntity) {
		ProductOfInterest productOfInterest = new ProductOfInterest();
		productOfInterest.setId(poiEntity.getKey().getId());
		productOfInterest.setCpf((String) poiEntity.getProperty(PROP_CPF));
		productOfInterest.setCustomerId((String) poiEntity.getProperty(PROP_CUSTOMER_ID));
		productOfInterest.setProductId((String) poiEntity.getProperty(PROP_PRODUCT_ID));
		productOfInterest.setTriggerPrice((float) poiEntity.getProperty(PROP_TRIGGER_PRICE));
		
		return productOfInterest;
	}

	private void productOfInterestToEntity (ProductOfInterest productOfInterest, Entity poiEntity) {
		poiEntity.setProperty(PROP_CPF, productOfInterest.getCpf());
		poiEntity.setProperty(PROP_CUSTOMER_ID, productOfInterest.getCustomerId());
		poiEntity.setProperty(PROP_PRODUCT_ID, productOfInterest.getProductId());
		poiEntity.setProperty(PROP_TRIGGER_PRICE, productOfInterest.getTriggerPrice());
	}
}
