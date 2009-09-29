/**
 * Copyright (c) 2008 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.sonatype.security.rest.users;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.plexus.rest.resource.PlexusResourceException;
import org.sonatype.security.rest.model.UserToRoleResource;
import org.sonatype.security.rest.model.UserToRoleResourceRequest;
import org.sonatype.security.usermanagement.NoSuchUserManagerException;
import org.sonatype.security.usermanagement.RoleIdentifier;
import org.sonatype.security.usermanagement.UserNotFoundException;

@Component( role = PlexusResource.class, hint = "UserToRolePlexusResource" )
public class UserToRolePlexusResource
    extends AbstractUserPlexusResource
{

    public static final String SOURCE_ID_KEY = "sourceId";

    public UserToRolePlexusResource()
    {
        this.setModifiable( true );
        this.setReadable( true );
    }

    @Override
    public Object getPayloadInstance()
    {
        return new UserToRoleResourceRequest();
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( getResourceUri(), "authcBasic,perms[security:users]" );
    }

    @Override
    public String getResourceUri()
    {
        return "/user_to_roles/{" + SOURCE_ID_KEY + "}/{" + USER_ID_KEY + "}";
    }

    protected String getUserId( Request request )
    {
        return request.getAttributes().get( USER_ID_KEY ).toString();
    }

    protected String getSourceId( Request request )
    {
        return request.getAttributes().get( SOURCE_ID_KEY ).toString();
    }

    /*
     * (non-Javadoc)
     * @see org.sonatype.plexus.rest.resource.AbstractPlexusResource#put(org.restlet.Context, org.restlet.data.Request,
     * org.restlet.data.Response, java.lang.Object)
     */
    @Override
    public Object put( Context context, Request request, Response response, Object payload )
        throws ResourceException
    {
        UserToRoleResourceRequest mappingRequest = (UserToRoleResourceRequest) payload;

        if ( mappingRequest.getData() == null )
        {
            throw new ResourceException(
                Status.CLIENT_ERROR_BAD_REQUEST,
                "User Role Mapping was not found in the Request." );
        }

        String userId = this.getUserId( request );
        String sourceId = this.getSourceId( request );

        // check if the user exists
        try
        {
            if ( this.getSecuritySystem().getUser( userId, sourceId ) == null )
            {
                throw new ResourceException( Status.CLIENT_ERROR_NOT_FOUND, "User with id '" + userId + "' not found." );
            }
        }
        catch ( UserNotFoundException e )
        {
            throw new ResourceException( Status.CLIENT_ERROR_NOT_FOUND, "User with id '" + userId + "' not found." );
        }
        catch ( NoSuchUserManagerException e )
        {
            this.getLogger().warn( e.getMessage(), e );
            throw new ResourceException( Status.CLIENT_ERROR_NOT_FOUND, "User with id '" + userId + "' not found." );
        }

        // get the dto
        UserToRoleResource userToRole = mappingRequest.getData();

        Set<RoleIdentifier> roleIdentifiers = this.restToSecurityModel( userToRole );

        if ( roleIdentifiers.size() == 0 )
        {
            throw new PlexusResourceException(
                Status.CLIENT_ERROR_BAD_REQUEST,
                "Configuration error.",
                getErrorResponse( "roles", "User requires one or more roles." ) );
        }

        try
        {
            // this will throw if we cannot find the user, in that case we will create one.
            getSecuritySystem().setUsersRoles( userToRole.getUserId(), userToRole.getSource(), roleIdentifiers );
        }
        catch ( InvalidConfigurationException e )
        {
            this.handleInvalidConfigurationException( e );
        }
        catch ( UserNotFoundException e )
        {
            throw new ResourceException( Status.CLIENT_ERROR_NOT_FOUND, "User with id '" + userId + "' not found." );
        }

        response.setStatus( Status.SUCCESS_NO_CONTENT );
        return null;
    }

    @Override
    public Object get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        String userId = this.getUserId( request );

        String sourceId = this.getSourceId( request );

        try
        {
            Set<RoleIdentifier> roleIds = getSecuritySystem().getUsersRoles( userId, sourceId );

            UserToRoleResourceRequest resp = new UserToRoleResourceRequest();

            resp.setData( securityToRestModel( userId, sourceId, roleIds ) );

            return resp;
        }
        catch ( UserNotFoundException e )
        {
            throw new ResourceException( Status.CLIENT_ERROR_NOT_FOUND, "Could not find user '" + userId + "'." );
        }
        catch ( NoSuchUserManagerException e )
        {
            throw new ResourceException( Status.CLIENT_ERROR_NOT_FOUND, "Could not find user manager for source '"
                + sourceId + "'." );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.sonatype.plexus.rest.resource.AbstractPlexusResource#delete(org.restlet.Context,
     * org.restlet.data.Request, org.restlet.data.Response)
     */
    @Override
    public void delete( Context context, Request request, Response response )
        throws ResourceException
    {
        // get the userId
        String userId = this.getUserId( request );
        String source = this.getSourceId( request );

        try
        {
            getSecuritySystem().setUsersRoles( userId, source, null );
        }
        catch ( InvalidConfigurationException e )
        {
            this.handleInvalidConfigurationException( e );
        }
        catch ( UserNotFoundException e )
        {
            throw new ResourceException( Status.CLIENT_ERROR_NOT_FOUND, "User with id '" + userId + "' not found." );
        }
    }

    private Set<RoleIdentifier> restToSecurityModel( UserToRoleResource restRoleMapping )
    {
        // FIXME: loss of roles source, currently we only support CRUDS on the XML realm but, that is temporary.

        Set<RoleIdentifier> roleIdentifiers = new HashSet<RoleIdentifier>();

        for ( String roleId : (List<String>) restRoleMapping.getRoles() )
        {
            roleIdentifiers.add( new RoleIdentifier( DEFAULT_SOURCE, roleId ) );
        }

        return roleIdentifiers;
    }

    private UserToRoleResource securityToRestModel( String userId, String source, Set<RoleIdentifier> roleIds )
    {
        UserToRoleResource resource = new UserToRoleResource();

        resource.setUserId( userId );

        resource.setSource( source );

        List<String> roles = new ArrayList<String>();

        for ( RoleIdentifier roleId : roleIds )
        {
            roles.add( roleId.getRoleId() );
        }

        resource.setRoles( roles );

        return resource;
    }
}
