/**
 * Copyright (c) 2002-2010 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.neo4j.server.rest.web;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.neo4j.server.database.Database;
import org.neo4j.server.rest.domain.AmpersandSeparatedList;
import org.neo4j.server.rest.domain.JsonHelper;
import org.neo4j.server.rest.domain.JsonParseException;
import org.neo4j.server.rest.domain.RelationshipDirection;
import org.neo4j.server.rest.domain.StorageActions.TraverserReturnType;
import org.neo4j.server.rest.domain.renderers.IndexRootRenderer;
import org.neo4j.server.rest.domain.renderers.JsonRenderers;
import org.neo4j.server.rest.domain.renderers.NodeRenderer;
import org.neo4j.server.rest.domain.renderers.NodesRenderer;
import org.neo4j.server.rest.domain.renderers.RelationshipRenderer;
import org.neo4j.server.rest.domain.renderers.RelationshipsRenderer;
import org.neo4j.server.rest.domain.renderers.RootRenderer;
import org.neo4j.server.rest.repr.BadInputException;

/* (non-javadoc)
 * I'd really like to split up the JSON and HTML parts in two different
 * classes, but Jersey can't handle that (2010-03-30).
 *
 * Instead we end up with json/html prefixed methods... URGH!
 * But it's... ok, kind of, since it doesn't really matter what the
 * methods are called. They only ones who could get upset are the tests.
 */
@Path( "/deprecated" )
public class JsonAndHtmlWebService extends GenericWebService
{


    public JsonAndHtmlWebService( @Context UriInfo uriInfo,
                                  @Context Database database )
    {
        super( uriInfo, database );
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonGetRoot() throws BadInputException
    {
        return getRoot( JsonRenderers.DEFAULT );
    }

    /**
     * Create an empty node.
     *
     * @param headers is used to derive what error to give the user if the body is
     *                non-empty. Pass null here if you're binding locally.
     * @param body    should be empty, this exists just so we can 400 on a request
     *                that isn't empty. Null is a good value here if you're binding
     *                locally.
     * @return the response from the request @
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path(PATH_NODES)
    public Response jsonCreateEmptyNode( @Context HttpHeaders headers, String body ) throws BadInputException
    {
        // This is a somewhat cumbersome hack. Because this resource does not
        // declare a @Consumes media type (and it shouldn't, since it
        // expects an empty request body), it accidentally becomes a catch-all
        // for requests meant for jsonCreateNode that have erroneous media
        // types. Those requests need to be handled differently from requests
        // with "correct" media types (text/plain or multipart/form-data).
        if ( !isNullOrEmpty( body ) && headers != null
                && (!headers.getMediaType().equals( MediaType.TEXT_PLAIN_TYPE ) && !headers.getMediaType().equals( MediaType.MULTIPART_FORM_DATA_TYPE )) )
        {
            // We've caught a request that was meant for jsonCreateNode, but
            // that had an erroneous media type.
            return Response.status( Status.UNSUPPORTED_MEDIA_TYPE ).build();
        }

        return createEmptyNode( body, JsonRenderers.DEFAULT );
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path(PATH_NODES)
    public Response jsonCreateNode( String json )
    {
        return createNode( json, JsonRenderers.DEFAULT );
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(PATH_NODE)
    public Response jsonGetNode( @PathParam("nodeId") long nodeId ) throws BadInputException
    {
        return getNode( nodeId, JsonRenderers.DEFAULT );
    }

    @PUT
    @Path(PATH_NODE_PROPERTIES)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response jsonSetNodeProperties( @PathParam("nodeId") long nodeId, String json )
    {
        return setNodeProperties( nodeId, json, JsonRenderers.DEFAULT );
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(PATH_NODE_PROPERTIES)
    public Response jsonGetNodeProperties( @PathParam("nodeId") long nodeId ) throws BadInputException
    {
        return getNodeProperties( nodeId, JsonRenderers.DEFAULT );
    }

    @DELETE
    @Path(PATH_NODE)
    public Response jsonDeleteNode( @PathParam("nodeId") long id )
    {
        return deleteNode( id, JsonRenderers.DEFAULT );
    }

    @PUT
    @Path(PATH_NODE_PROPERTY)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response jsonSetNodeProperty( @PathParam("nodeId") long id, @PathParam("key") String key, String json )
    {
        return setNodeProperty( id, key, json, JsonRenderers.DEFAULT );
    }

    @GET
    @Path(PATH_NODE_PROPERTY)
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonGetNodeProperty( @PathParam("nodeId") long nodeId, @PathParam("key") String key ) throws JsonParseException
    {
        return getNodeProperty( nodeId, key, JsonRenderers.DEFAULT );
    }

    @DELETE
    @Path(PATH_NODE_PROPERTIES)
    public Response jsonRemoveNodeProperties( @PathParam("nodeId") long nodeId )
    {
        return removeNodeProperties( nodeId, JsonRenderers.DEFAULT );
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path(PATH_NODE_RELATIONSHIPS)
    public Response jsonCreateRelationship( @PathParam("nodeId") long startNodeId, String json ) throws BadInputException
    {
        return createRelationship( startNodeId, json, JsonRenderers.DEFAULT );
    }

    @DELETE
    @Path(PATH_NODE_PROPERTY)
    public Response jsonRemoveNodeProperty( @PathParam("nodeId") long nodeId, @PathParam("key") String key )
    {
        return removeNodeProperty( nodeId, key, JsonRenderers.DEFAULT );
    }

    @GET
    @Path(PATH_RELATIONSHIP)
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonGetRelationship( @PathParam("relationshipId") long relationshipId ) throws BadInputException
    {
        return getRelationship( relationshipId, JsonRenderers.DEFAULT );
    }

    @GET
    @Path(PATH_RELATIONSHIP_PROPERTIES)
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonGetRelationshipProperties( @PathParam("relationshipId") long relationshipId ) throws BadInputException
    {
        return getRelationshipProperties( relationshipId, JsonRenderers.DEFAULT );
    }

    @GET
    @Path(PATH_RELATIONSHIP_PROPERTY)
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonGetRelationshipProperty( @PathParam("relationshipId") long relationshipId, @PathParam("key") String key ) throws JsonParseException
    {
        return getRelationshipProperty( relationshipId, key, JsonRenderers.DEFAULT );
    }

    @DELETE
    @Path(PATH_RELATIONSHIP)
    public Response jsonRemoveRelationship( @PathParam("relationshipId") long relationshipId )
    {
        return removeRelationship( relationshipId, JsonRenderers.DEFAULT );
    }

    @GET
    @Path(PATH_NODE_RELATIONSHIPS_W_DIR)
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonGetRelationships( @PathParam("nodeId") long nodeId, @PathParam("direction") RelationshipDirection direction ) throws BadInputException
    {
        return getRelationships( nodeId, direction, new AmpersandSeparatedList(), JsonRenderers.ARRAY );
    }

    @GET
    @Path(PATH_NODE_RELATIONSHIPS_W_DIR_N_TYPES)
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonGetRelationships( @PathParam("nodeId") long nodeId, @PathParam("direction") RelationshipDirection direction,
                                          @PathParam("types") AmpersandSeparatedList types ) throws BadInputException
    {
        return getRelationships( nodeId, direction, types, JsonRenderers.ARRAY );
    }

    @PUT
    @Path(PATH_RELATIONSHIP_PROPERTIES)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response jsonSetRelationshipProperties( @PathParam("relationshipId") long relationshipId, String json )
    {
        return setRelationshipProperties( relationshipId, json, JsonRenderers.DEFAULT );
    }

    @PUT
    @Path(PATH_RELATIONSHIP_PROPERTY)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response jsonSetRelationshipProperty( @PathParam("relationshipId") long relationshipId, @PathParam("key") String key, String json )

    {
        return setRelationshipProperty( relationshipId, key, json, JsonRenderers.DEFAULT );
    }

    @DELETE
    @Path(PATH_RELATIONSHIP_PROPERTIES)
    public Response jsonRemoveRelationshipProperties( @PathParam("relationshipId") long relationshipId )
    {
        return removeRelationshipProperties( relationshipId, JsonRenderers.DEFAULT );
    }

    @DELETE
    @Path(PATH_RELATIONSHIP_PROPERTY)
    public Response jsonRemoveRelationshipProperty( @PathParam("relationshipId") long relationshipId, @PathParam("key") String propertyKey )
    {
        return removeRelationshipProperty( relationshipId, propertyKey, JsonRenderers.DEFAULT );
    }

    @GET
    @Path(PATH_NODE_INDEX)
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonGetNodeIndexRoot() throws BadInputException
    {
        return getNodeIndexRoot( JsonRenderers.DEFAULT );
    }

    @POST
    @Path(PATH_NODE_INDEX)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response jsonCreateNodeIndex( String json )
    {
        return createNodeIndex( json, JsonRenderers.DEFAULT );
    }

    @POST
    @Path(PATH_RELATIONSHIP_INDEX)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response jsonCreateRelationshipIndex( String json )
    {
        return createRelationshipIndex( json, JsonRenderers.DEFAULT );
    }

    @GET
    @Path(PATH_RELATIONSHIP_INDEX)
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonGetRelationshipIndexRoot() throws BadInputException
    {
        return getRelationshipIndexRoot( JsonRenderers.DEFAULT );
    }

    @POST
    @Path(PATH_NODE_INDEX_QUERY)
    @Consumes(MediaType.TEXT_PLAIN)
    public Response jsonAddToNodeIndexPlainText( @PathParam("indexName") String indexName, @PathParam("key") String key, @PathParam("value") String value,
                                                 String objectUri ) throws JsonParseException
    {
        return addToIndex( IndexType.NODE, indexName, key, value, JsonHelper.createJsonFrom( objectUri ), JsonRenderers.DEFAULT );
    }

    @POST
    @Path(PATH_NODE_INDEX_QUERY)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response jsonAddToNodeIndex( @PathParam("indexName") String indexName, @PathParam("key") String key, @PathParam("value") String value, String objectUri )
    {
        return addToIndex( IndexType.NODE, indexName, key, value, objectUri, JsonRenderers.DEFAULT );
    }

    @POST
    @Path(PATH_RELATIONSHIP_INDEX_QUERY)
    @Consumes(MediaType.TEXT_PLAIN)
    public Response jsonAddToRelationshipIndexPlainText( @PathParam("indexName") String indexName, @PathParam("key") String key, @PathParam("value") String value,
                                                         String objectUri ) throws JsonParseException
    {
        return addToIndex( IndexType.RELATIONSHIP, indexName, key, value, JsonHelper.createJsonFrom( objectUri ), JsonRenderers.DEFAULT );
    }

    @POST
    @Path(PATH_RELATIONSHIP_INDEX_QUERY)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response jsonAddToRelationshipIndex( @PathParam("indexName") String indexName, @PathParam("key") String key, @PathParam("value") String value, String objectUri )
    {
        return addToIndex( IndexType.RELATIONSHIP, indexName, key, value, objectUri, JsonRenderers.DEFAULT );
    }


    @GET
    @Path(PATH_NODE_INDEX_ID)
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonGetObjectFromNodeIndexUri( @PathParam("indexName") String indexName, @PathParam("key") String key, @PathParam("value") String value,
                                                   @PathParam("id") long id )
    {
        return getObjectFromIndexUri( IndexType.NODE, indexName, key, value, id, JsonRenderers.DEFAULT );
    }

    @GET
    @Path(PATH_RELATIONSHIP_INDEX_ID)
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonGetObjectFromRelationshipIndexUri( @PathParam("indexName") String indexName, @PathParam("key") String key, @PathParam("value") String value,
                                                           @PathParam("id") long id )
    {
        return getObjectFromIndexUri( IndexType.RELATIONSHIP, indexName, key, value, id, JsonRenderers.DEFAULT );
    }

    @GET
    @Path(PATH_NODE_INDEX_QUERY)
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonGetIndexedNodes( @PathParam("indexName") String indexName, @PathParam("key") String key, @PathParam("value") String value ) throws BadInputException

    {
        return getIndexedNodes( indexName, key, value, JsonRenderers.ARRAY );
    }


    @DELETE
    @Path(PATH_NODE_INDEX_ID)
    public Response jsonRemoveFromNodeIndex( @PathParam("indexName") String indexName, @PathParam("key") String key, @PathParam("value") String value,
                                             @PathParam("id") long id )
    {
        return removeFromIndex( IndexType.NODE, indexName, key, value, id, JsonRenderers.DEFAULT );
    }

    @DELETE
    @Path(PATH_RELATIONSHIP_INDEX_ID)
    public Response jsonRemoveFromRelationshipIndex( @PathParam("indexName") String indexName, @PathParam("key") String key, @PathParam("value") String value,
                                                     @PathParam("id") long id )
    {
        return removeFromIndex( IndexType.RELATIONSHIP, indexName, key, value, id, JsonRenderers.DEFAULT );
    }

    @POST
    @Path(PATH_NODE_TRAVERSE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonTraverse( @PathParam("nodeId") long startNode, @PathParam("returnType") TraverserReturnType returnType, String description ) throws BadInputException
    {
        return traverse( startNode, returnType, description, JsonRenderers.ARRAY );
    }

    @POST
    @Path(PATH_NODE_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonPath( @PathParam("nodeId") long startNode, String description ) throws BadInputException
    {
        return path( startNode, description, JsonRenderers.DEFAULT );
    }

    @POST
    @Path(PATH_NODE_PATHS)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonPaths( @PathParam("nodeId") long startNode, String description ) throws BadInputException
    {
        return paths( startNode, description, JsonRenderers.ARRAY );
    }

    @GET
    @Path(PATH_RELATIONSHIP_INDEX_QUERY)
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonGetIndexedRelationships( @PathParam("indexName") String indexName, @PathParam("key") String key, @PathParam("value") String value ) throws BadInputException
    {
        return getIndexedRelationships( indexName, key, value, JsonRenderers.ARRAY );
    }

    // ===== HTML =====

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response htmlGetRoot() throws BadInputException
    {
        return getRoot( new RootRenderer() );
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path(PATH_NODE)
    public Response htmlGetNode( @PathParam("nodeId") long nodeId ) throws BadInputException

    {
        return getNode( nodeId, new NodeRenderer( database.graph.getRelationshipTypes() ) );
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path(PATH_NODE_RELATIONSHIPS_W_DIR)
    public Response htmlGetRelationships( @PathParam("nodeId") long nodeId, @PathParam("direction") RelationshipDirection direction ) throws BadInputException

    {
        return getRelationships( nodeId, direction, new AmpersandSeparatedList(), new RelationshipsRenderer() );
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path(PATH_NODE_RELATIONSHIPS_W_DIR_N_TYPES)
    public Response htmlGetRelationships( @PathParam("nodeId") long nodeId, @PathParam("direction") RelationshipDirection direction,
                                          @PathParam("types") AmpersandSeparatedList types ) throws BadInputException

    {
        return getRelationships( nodeId, direction, types, new RelationshipsRenderer() );
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path(PATH_RELATIONSHIP)
    public Response htmlGetRelationship( @PathParam("relationshipId") long relId ) throws BadInputException

    {
        return getRelationship( relId, new RelationshipRenderer() );
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path(PATH_NODE_INDEX)
    public Response htmlGetNodeIndexRoot() throws BadInputException
    {
        return getNodeIndexRoot( new IndexRootRenderer() );
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path(PATH_RELATIONSHIP_INDEX)
    public Response htmlGetRelationshipIndexRoot() throws BadInputException
    {
        return getRelationshipIndexRoot( new IndexRootRenderer() );
    }

    @GET
    @Path(PATH_NODE_INDEX_QUERY)
    @Produces(MediaType.TEXT_HTML)
    public Response htmlGetIndexedNodes( @PathParam("indexName") String indexName, @PathParam("key") String key, @PathParam("value") String value ) throws BadInputException
    {
        return getIndexedNodes( indexName, key, value, new NodesRenderer() );
    }

    @GET
    @Path(PATH_RELATIONSHIP_INDEX_QUERY)
    @Produces(MediaType.TEXT_HTML)
    public Response htmlGetIndexedRelationships( @PathParam("indexName") String indexName, @PathParam("key") String key, @PathParam("value") String value ) throws BadInputException
    {
        return getIndexedRelationships( indexName, key, value, new RelationshipRenderer() );
    }

}
