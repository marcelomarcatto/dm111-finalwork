package br.inatel.t124.dm111.finalwork.services;

import java.io.IOException;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.gson.Gson;

import br.inatel.t124.dm111.finalwork.models.OrderStatus;
import br.inatel.t124.dm111.finalwork.models.User;

@Path("/ordersStatus")
public class OrderStatusManager {
	
	private static final Logger log = Logger.getLogger("OrderStatusManager");
	
	@Context
	SecurityContext securityContext;
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed({"ADMIN"})
	public String send(@Valid OrderStatus orderStatus) {
		
		User user = getUserByCPF(orderStatus.getCpf());
		
		Sender sender = new Sender("AIzaSyCkVGqGPF00ueJnDXVPvj_JAEMJZmwr4eY");	
		Gson gson = new Gson();
		Message message = new Message.Builder().addData("orderStatus", gson.toJson(orderStatus)).build();			
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
		return "Order status " + orderStatus.getOrderStatus() + " sent to user " + user.getEmail();
	}
	
	private User getUserByCPF(String cpf) throws WebApplicationException {		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Filter emailFilter = new FilterPredicate("cpf", FilterOperator.EQUAL, cpf);
		
		Query query = new Query("Users").setFilter(emailFilter);
		Entity userEntity = datastore.prepare(query).asSingleEntity();
		
		if (userEntity == null) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		
		return UserManager.entityToUser(userEntity);
	}
}
