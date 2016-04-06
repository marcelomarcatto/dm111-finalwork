package br.inatel.t124.dm111.finalwork.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
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

import br.inatel.t124.dm111.finalwork.models.User;

@Path("/users")
public class UserManager {
	
	private static final Logger log = Logger.getLogger("UserManager");
	
	public static final String USER_KIND = "Users"; 
	
	public static final String PROP_EMAIL = "email";
	public static final String PROP_PASSWORD = "password";
	public static final String PROP_GCM_REG_ID = "gcmRegId";
	public static final String PROP_LAST_LOGIN = "lastLogin";
	public static final String PROP_LAST_GCM_REGISTER = "lastGCMRegister";
	public static final String PROP_ROLE = "role";
	public static final String PROP_CPF = "cpf";
	public static final String PROP_CUSTOMER_ID = "customerId";
	public static final String PROP_CUSTOMER_CRM_ID = "customerCRMId";
	
	@Context
	SecurityContext securityContext;
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({"ADMIN"})
	public List<User> getList() {

		List<User> users = new ArrayList<>();
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		Query query = new Query(USER_KIND).addSort(PROP_EMAIL, SortDirection.ASCENDING);
		List<Entity> userEntities = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());

		for (Entity userEntity : userEntities) {
			User user = entityToUser(userEntity);	
			users.add(user);
		}
		
		return users;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({"ADMIN", "USER"})
	@Path("/{email}")
	public User get(@PathParam(PROP_EMAIL) String email) {

		if (securityContext.getUserPrincipal().getName().equals(email) || securityContext.isUserInRole("ADMIN")) {
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

			Filter emailFilter = new FilterPredicate(PROP_EMAIL, FilterOperator.EQUAL, email);
			Query query = new Query(USER_KIND).setFilter(emailFilter);
			Entity userEntity = datastore.prepare(query).asSingleEntity();
			
			if (userEntity != null) {
				User user = entityToUser(userEntity);
				return user;
			} else {
				throw new WebApplicationException(Status.NOT_FOUND);
			}			
		} else {
			throw new WebApplicationException(Status.FORBIDDEN);
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@PermitAll
	public User insert(@Valid User user) {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		if (!checkIfEmailExist(user)) {
			if (!checkIfCpfExist(user)) {
				if (!securityContext.isUserInRole("ADMIN")) {
					user.setRole("USER");
				}
				Key userKey = KeyFactory.createKey(USER_KIND, "userKey");
				Entity userEntity = new Entity(USER_KIND, userKey);

				user.setGcmRegId("");
				user.setLastGCMRegister(null);
				user.setLastLogin(null);
				userToEntity(user, userEntity);

				datastore.put(userEntity);
				user.setId(userEntity.getKey().getId());
			} else {
				throw new WebApplicationException("An user with CPF " + user.getCpf() + " already exists.",
						Status.BAD_REQUEST);
			}
		} else {
			throw new WebApplicationException("An user with email " + user.getEmail() + " already exists.",
					Status.BAD_REQUEST);
		}
		
		return user;
	}

	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{email}")
	@RolesAllowed({"ADMIN", "USER"})
	public User update(@PathParam("email") String email, @Valid User user) {
		if (user.getId() != 0) {
			if (securityContext.getUserPrincipal().getName().equals(email) || securityContext.isUserInRole("ADMIN")) {
				if (!checkIfEmailExist(user)) {
					if (!checkIfCpfExist(user)) {

						DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

						Filter emailFilter = new FilterPredicate(PROP_EMAIL, FilterOperator.EQUAL, email);

						Query query = new Query(USER_KIND).setFilter(emailFilter);
						Entity userEntity = datastore.prepare(query).asSingleEntity();

						if (userEntity != null) {
							userToEntity(user, userEntity);

							if (!securityContext.isUserInRole("ADMIN")) {
								user.setRole("USER");
							}
							datastore.put(userEntity);

							return user;
						} else {
							throw new WebApplicationException(Status.NOT_FOUND);
						}
					} else {
						throw new WebApplicationException("An user with CPF " + user.getCpf() + " already exists.",
								Status.BAD_REQUEST);
					}
				} else {
					throw new WebApplicationException("An user with email " + user.getEmail() + " already exists.",
							Status.BAD_REQUEST);
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
	public User delete(@PathParam("cpf") String email) {

		if (securityContext.getUserPrincipal().getName().equals(email) || securityContext.isUserInRole("ADMIN")) {
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
			Filter emailFilter = new FilterPredicate(PROP_CPF, FilterOperator.EQUAL, email);
			Query query = new Query(USER_KIND).setFilter(emailFilter);
		
			Entity userEntity = datastore.prepare(query).asSingleEntity();				
			if (userEntity != null) {
				datastore.delete(userEntity.getKey());
				User user = entityToUser(userEntity);
				
				return user;
			} else {	
				throw new WebApplicationException(Status.NOT_FOUND);			
			}
		} else {
			throw new WebApplicationException(Status.FORBIDDEN);
		}			
	}

	private boolean checkIfEmailExist (User user) {		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Filter emailFilter = new FilterPredicate(PROP_EMAIL, FilterOperator.EQUAL, user.getEmail());
		
		Query query = new Query(USER_KIND).setFilter(emailFilter);
		Entity userEntity = datastore.prepare(query).asSingleEntity();
	
		if (userEntity == null) {
			return false;
		} else {
			if (userEntity.getKey().getId() == user.getId()) {
				return false;
			} else {				
				return true;
			}				
		}
	}
	
	private boolean checkIfCpfExist (User user) {		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Filter emailFilter = new FilterPredicate(PROP_CPF, FilterOperator.EQUAL, user.getCpf());
		
		Query query = new Query(USER_KIND).setFilter(emailFilter);
		Entity userEntity = datastore.prepare(query).asSingleEntity();
	
		if (userEntity == null) {
			return false;
		} else {
			if (userEntity.getKey().getId() == user.getId()) {
				return false;
			} else {				
				return true;
			}				
		}
	}

	public static User entityToUser (Entity userEntity) {
		User user = new User();
		user.setId(userEntity.getKey().getId());
		user.setEmail((String) userEntity.getProperty(PROP_EMAIL));
		user.setPassword((String) userEntity.getProperty(PROP_PASSWORD));
		user.setGcmRegId((String) userEntity.getProperty(PROP_GCM_REG_ID));
		user.setLastLogin((Date) userEntity.getProperty(PROP_LAST_LOGIN));
		user.setLastGCMRegister((Date) userEntity.getProperty(PROP_LAST_GCM_REGISTER));
		user.setRole((String) userEntity.getProperty(PROP_ROLE));
		
		return user;
	}

	private void userToEntity (User user, Entity userEntity) {
		userEntity.setProperty(PROP_EMAIL, user.getEmail());
		userEntity.setProperty(PROP_PASSWORD, user.getPassword());
		userEntity.setProperty(PROP_GCM_REG_ID, user.getGcmRegId());
		userEntity.setProperty(PROP_LAST_LOGIN, user.getLastLogin());
		userEntity.setProperty(PROP_LAST_GCM_REGISTER, user.getLastGCMRegister());
		userEntity.setProperty(PROP_ROLE, user.getRole());
	}


}