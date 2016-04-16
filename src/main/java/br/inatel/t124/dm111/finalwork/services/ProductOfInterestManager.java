package br.inatel.t124.dm111.finalwork.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

import br.inatel.t124.dm111.finalwork.models.ProductOfInterest;
import br.inatel.t124.dm111.finalwork.models.User;

@Path("/productsOfInterest")
public class ProductOfInterestManager {
	
	private static final Logger log = Logger.getLogger("ProductOfInterestManager");

	public static final String PRODUCTS_INTEREST_KIND = "ProductsOfInterest";

	public static final String PROP_CPF = "cpf";
	public static final String PROP_CUSTOMER_ID = "customerId";
	public static final String PROP_PRODUCT_ID = "productId";
	public static final String PROP_TRIGGER_PRICE = "triggerPrice";

	@Context
	SecurityContext securityContext;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{cpf}")
	@RolesAllowed({ "ADMIN", "USER" })
	public List<ProductOfInterest> getList(@PathParam("cpf") String cpf) {

		validateByCPF(cpf);

		List<ProductOfInterest> productsOfInterest = new ArrayList<>();
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		Filter cpfFilter = new FilterPredicate(PROP_CPF, FilterOperator.EQUAL, cpf);
		Query query = new Query(PRODUCTS_INTEREST_KIND).setFilter(cpfFilter);
		List<Entity> poiEntities = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());

		if (poiEntities != null) {
			for (Entity poiEntity : poiEntities) {
				ProductOfInterest productOfInterest = entityToProductOfInterest(poiEntity);
				productsOfInterest.add(productOfInterest);
			}
		}

		if (productsOfInterest.size() > 0) {
			return productsOfInterest;
		} else {
			throw new WebApplicationException(Status.NOT_FOUND);
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed({ "ADMIN", "USER" })
	public ProductOfInterest insert(@Valid ProductOfInterest productOfInterest) {

		validateByCPF(productOfInterest.getCpf());

		Entity poiEntity = getByCpfAndProductId(productOfInterest.getCpf(), productOfInterest.getProductId());

		if (poiEntity == null) {
			Key poiKey = KeyFactory.createKey(PRODUCTS_INTEREST_KIND, "productOfInterestKey");
			poiEntity = new Entity(PRODUCTS_INTEREST_KIND, poiKey);
		}

		productOfInterestToEntity(productOfInterest, poiEntity);

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		datastore.put(poiEntity);
		productOfInterest.setId(poiEntity.getKey().getId());

		return productOfInterest;
	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{cpf}/{productId}")
	@RolesAllowed({ "ADMIN", "USER" })
	public ProductOfInterest delete(@PathParam("cpf") String cpf, @PathParam("productId") String productId) {

		validateByCPF(cpf);

		Entity poiEntity = getByCpfAndProductId(cpf, productId);

		if (poiEntity != null) {
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			datastore.delete(poiEntity.getKey());
			return entityToProductOfInterest(poiEntity);
		} else {
			throw new WebApplicationException(Status.NOT_FOUND);
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{productId}/{newPrice}")
	@RolesAllowed({ "ADMIN" })
	public String submitPriceChange(@PathParam("productId") String productId, @PathParam("newPrice") float newPrice) {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		Filter productIdFilter = new FilterPredicate(PROP_PRODUCT_ID, FilterOperator.EQUAL, productId);
		Query query = new Query(PRODUCTS_INTEREST_KIND).setFilter(productIdFilter);
		List<Entity> poiEntities = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());

		int sentCount = 0;
		if (poiEntities != null && poiEntities.size() > 0) {
			for (Entity poiEntity : poiEntities) {

				ProductOfInterest productOfInterest = entityToProductOfInterest(poiEntity);
				if (newPrice <= productOfInterest.getTriggerPrice()) {
					try {
						sendGcmMessage(productOfInterest, newPrice);
						sentCount++;
					} catch (Exception e) {
						log.warning("Message was not sent to cpf " + productOfInterest.getCpf() + ". Error: "
								+ e.getMessage());
					}
				}
			}
		} else {
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		return "Message sent to " + sentCount + " users";
	}

	private Entity getByCpfAndProductId(String cpf, String productId) {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		Filter cpfFilter = new FilterPredicate(PROP_CPF, FilterOperator.EQUAL, cpf);
		Filter productIdFilter = new FilterPredicate(PROP_PRODUCT_ID, FilterOperator.EQUAL, productId);
		Filter compositeFilter = CompositeFilterOperator.and(cpfFilter, productIdFilter);

		Query query = new Query(PRODUCTS_INTEREST_KIND).setFilter(compositeFilter);
		return datastore.prepare(query).asSingleEntity();
	}

	private boolean validateByCPF(String cpf) {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Filter cpfFilter = new FilterPredicate(UserManager.PROP_CPF, FilterOperator.EQUAL, cpf);

		Query query = new Query(UserManager.USER_KIND).setFilter(cpfFilter);
		Entity userEntity = datastore.prepare(query).asSingleEntity();

		if (userEntity != null
				&& securityContext.getUserPrincipal().getName().equals(userEntity.getProperty(UserManager.PROP_EMAIL))
				|| securityContext.isUserInRole("ADMIN")) {
			return true;
		} else {
			throw new WebApplicationException(Status.FORBIDDEN);
		}
	}

	private User getUserByCPF(String cpf) {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Filter emailFilter = new FilterPredicate("cpf", FilterOperator.EQUAL, cpf);

		Query query = new Query("Users").setFilter(emailFilter);
		Entity userEntity = datastore.prepare(query).asSingleEntity();

		if (userEntity == null) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		return UserManager.entityToUser(userEntity);
	}

	private void sendGcmMessage(ProductOfInterest productOfInterest, float newPrice) {
		User user = getUserByCPF(productOfInterest.getCpf());

		Sender sender = new Sender("AIzaSyCkVGqGPF00ueJnDXVPvj_JAEMJZmwr4eY");
		Message message = new Message.Builder().addData("Product of Interest",
				"The product Id " + productOfInterest.getProductId() + " now has value " + newPrice).build();
		Result result;

		try {
			result = sender.send(message, user.getGcmRegId(), 5);
			if (result.getMessageId() != null) {
				String canonicalRegId = result.getCanonicalRegistrationId();
				if (canonicalRegId != null) {
					log.severe("User [" + user.getEmail() + "] with more than one registry");
				}
			} else {	
				String error = result.getErrorCodeName();
				log.severe("User [" + user.getEmail() + "] not registered.");
				log.severe(error);
				throw new WebApplicationException(Status.NOT_FOUND);					
			}
		} catch (IOException e) {
			throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
		}
	}

	public static ProductOfInterest entityToProductOfInterest(Entity poiEntity) {
		ProductOfInterest productOfInterest = new ProductOfInterest();
		productOfInterest.setId(poiEntity.getKey().getId());
		productOfInterest.setCpf((String) poiEntity.getProperty(PROP_CPF));
		productOfInterest.setCustomerId((String) poiEntity.getProperty(PROP_CUSTOMER_ID));
		productOfInterest.setProductId((String) poiEntity.getProperty(PROP_PRODUCT_ID));
		productOfInterest.setTriggerPrice(Float.parseFloat(poiEntity.getProperty(PROP_TRIGGER_PRICE).toString()));

		return productOfInterest;
	}

	private void productOfInterestToEntity(ProductOfInterest productOfInterest, Entity poiEntity) {
		poiEntity.setProperty(PROP_CPF, productOfInterest.getCpf());
		poiEntity.setProperty(PROP_CUSTOMER_ID, productOfInterest.getCustomerId());
		poiEntity.setProperty(PROP_PRODUCT_ID, productOfInterest.getProductId());
		poiEntity.setProperty(PROP_TRIGGER_PRICE, productOfInterest.getTriggerPrice());
	}
}
