package io.openshift.booster;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class BoosterApplicationService {

	@GET
    @Path("/")
	public String applicationIsReady() {
		return "<h1>Hi, Your Application is Ready</h1>";
	}

}
