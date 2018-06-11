package com.idt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;

// This API is accessible from /api/idt. The endpoint paths specified below are relative to it.
@Path("idt")
public class DiamondChainApi {
    private final CordaRPCOps rpcOps;
    private final CordaX500Name myLegalName;

    private final List<String> serviceNames = ImmutableList.of("Notary", "Network Map Service");

    static private final Logger logger = LoggerFactory.getLogger(DiamondChainApi.class);

    public DiamondChainApi(CordaRPCOps services) {
        this.rpcOps = services;
        this.myLegalName = rpcOps.nodeInfo().getLegalIdentities().get(0).getName();
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, CordaX500Name> whoami() {
        return ImmutableMap.of("me", myLegalName);
    }

    /**
     * Accessible at /api/idt/approve/{id}.
     */
    @GET
    @Path("/approve/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response approve(@PathParam("id") String id) {
        try{
            final SignedTransaction signedTx = rpcOps
                    .startTrackedFlowDynamic(DiamondApproveFlow.Initiator.class, id, true)
                    .getReturnValue()
                    .get();

            final String msg = String.format("Transaction id %s committed to ledger.\n", signedTx.getId());
            return Response.status(CREATED).entity(msg).build();


        }catch (Throwable ex){
            final String msg = ex.getMessage();
            logger.error(msg, ex);
            return Response.status(BAD_REQUEST).entity(msg).build();
        }
    }

    /**
     * Accessible at /api/idt/decline/{id}.
     */
    @GET
    @Path("/decline/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response decline(@PathParam("id") String id) {
        try{
            final SignedTransaction signedTx = rpcOps
                    .startTrackedFlowDynamic(DiamondApproveFlow.Initiator.class, id, false)
                    .getReturnValue()
                    .get();

            final String msg = String.format("Transaction id %s committed to ledger.\n", signedTx.getId());
            return Response.status(CREATED).entity(msg).build();


        }catch (Throwable ex){
            final String msg = ex.getMessage();
            logger.error(msg, ex);
            return Response.status(BAD_REQUEST).entity(msg).build();
        }
    }

    /**
     * Accessible at /api/idt/diamonds.
     */
    @GET
    @Path("diamonds")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<DiamondAssetState>> getDiamonds() {
        return rpcOps.vaultQuery(DiamondAssetState.class).getStates();
    }

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<CordaX500Name>> getPeers() {
        List<NodeInfo> nodeInfoSnapshot = rpcOps.networkMapSnapshot();
        return ImmutableMap.of("peers", nodeInfoSnapshot
                .stream()
                .map(node -> node.getLegalIdentities().get(0).getName())
                //.filter(name -> !name.equals(myLegalName) && !serviceNames.contains(name.getOrganisation()))
                .collect(toList()));
    }

    /**
     * Accessible at /api/idt/create.
     *
     */
    @PUT
    @Path("create")
    //@Produces(MediaType.APPLICATION_JSON)
    public Response create(@QueryParam("externalId") String id,
                           @QueryParam("description") String description, @QueryParam("carats") double carats,
                           @QueryParam("cost") double cost, @QueryParam("percent") float percent,
                           @QueryParam("approver") CordaX500Name approverName) throws InterruptedException, ExecutionException {
        System.out.println(String.format("Approver name is %s", approverName));
        if (cost <= 0) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'cost' must be non-negative.\n").build();
        }
        if (percent <= 0) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'percent' must be non-negative.\n").build();
        }
        if (carats <= 0) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'carats' must be non-negative.\n").build();
        }
        if (approverName == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'approverName' missing or has wrong format.\n").build();
        }

        final Party approverParty = rpcOps.wellKnownPartyFromX500Name(approverName);
        if (approverParty == null) {
            return Response.status(BAD_REQUEST).entity("Party named " + approverName + "cannot be found.\n").build();
        }

        try {
            final SignedTransaction signedTx = rpcOps
                    .startTrackedFlowDynamic(DiamondCreateFlow.Initiator.class, id, description,
                            carats, cost, percent, approverParty)
                    .getReturnValue()
                    .get();

            final String msg = String.format("Transaction id %s committed to ledger.\n", signedTx.getId());
            return Response.status(CREATED).entity(msg).build();

        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            logger.error(msg, ex);
            return Response.status(BAD_REQUEST).entity(msg).build();
        }
    }

    /**
     * Accessible at /api/idt/transfer.
     *
     */
    @PUT
    @Path("transfer")
    //@Produces(MediaType.APPLICATION_JSON)
    public Response transfer(@QueryParam("externalId") String id, @QueryParam("newOwnerName") CordaX500Name newOwnerName){
        final Party newOwnerParty = rpcOps.wellKnownPartyFromX500Name(newOwnerName);
        if (newOwnerParty == null) {
            return Response.status(BAD_REQUEST).entity("Party named " + newOwnerName + "cannot be found.\n").build();
        }

        try {
            final SignedTransaction signedTx = rpcOps
                    .startTrackedFlowDynamic(DiamondCreateFlow.Initiator.class, id, newOwnerParty)
                    .getReturnValue()
                    .get();

            final String msg = String.format("Transaction id %s committed to ledger.\n", signedTx.getId());
            return Response.status(OK).entity(msg).build();

        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            logger.error(msg, ex);
            return Response.status(BAD_REQUEST).entity(msg).build();
        }
    }
}