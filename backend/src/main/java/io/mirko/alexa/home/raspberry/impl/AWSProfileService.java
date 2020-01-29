package io.mirko.alexa.home.raspberry.impl;


import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/user")
@RegisterRestClient
public interface AWSProfileService {
    @GET
    @Path("/profile")
    @Produces("application/json")
    AWSProfile getProfile(@HeaderParam("x-amz-access-token") String accessToken);
}
