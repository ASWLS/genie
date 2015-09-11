/*
 *
 *  Copyright 2015 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.genie.web.controllers;

import com.netflix.genie.common.exceptions.GenieException;
import com.netflix.genie.common.model.Application;
import com.netflix.genie.common.model.Cluster;
import com.netflix.genie.common.model.ClusterStatus;
import com.netflix.genie.common.model.Command;
import com.netflix.genie.common.model.CommandStatus;
import com.netflix.genie.core.services.CommandConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.HttpURLConnection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Code for CommandConfigResource.
 *
 * @author amsharma
 * @author tgianos
 */
@RestController
@RequestMapping(value = "/api/v3/commands", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "/api/v3/commands", tags = "commands", description = "Manage the available commands")
public final class CommandController {

    private static final Logger LOG = LoggerFactory.getLogger(CommandController.class);

    private final CommandConfigService commandConfigService;

    /**
     * Constructor.
     *
     * @param commandConfigService The command configuration service to use.
     */
    @Autowired
    public CommandController(final CommandConfigService commandConfigService) {
        this.commandConfigService = commandConfigService;
    }

    /**
     * Create a Command configuration.
     *
     * @param command The command configuration to create
     * @return The command created
     * @throws GenieException For any error
     */
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(
            value = "Create a command",
            notes = "Create a command from the supplied information.",
            response = Command.class
    )
    @ApiResponses(value = {
            @ApiResponse(
                    code = HttpURLConnection.HTTP_CREATED,
                    message = "Created",
                    response = Command.class
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_CONFLICT,
                    message = "A command with the supplied id already exists"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_PRECON_FAILED,
                    message = "Invalid required parameter supplied"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_INTERNAL_ERROR,
                    message = "Genie Server Error due to Unknown Exception"
            )
    })
    public ResponseEntity<Command> createCommand(
            @ApiParam(
                    value = "The command to create.",
                    required = true
            )
            @RequestBody
            final Command command
    ) throws GenieException {
        LOG.info("called to create new command configuration " + command.toString());
        final Command createdCommand = this.commandConfigService.createCommand(command);
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setLocation(
                ServletUriComponentsBuilder
                        .fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(createdCommand.getId())
                        .toUri()
        );
        return new ResponseEntity<>(createdCommand, httpHeaders, HttpStatus.CREATED);
    }

    /**
     * Get Command configuration for given id.
     *
     * @param id unique id for command configuration
     * @return The command configuration
     * @throws GenieException For any error
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ApiOperation(
            value = "Find a command by id",
            notes = "Get the command by id if it exists",
            response = Command.class
    )
    @ApiResponses(value = {
            @ApiResponse(
                    code = HttpURLConnection.HTTP_NOT_FOUND,
                    message = "Command not found"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_PRECON_FAILED,
                    message = "Invalid required parameter supplied"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_INTERNAL_ERROR,
                    message = "Genie Server Error due to Unknown Exception"
            )
    })
    public Command getCommand(
            @ApiParam(
                    value = "Id of the command to get.",
                    required = true
            )
            @PathVariable("id")
            final String id
    ) throws GenieException {
        LOG.info("Called to get command with id " + id);
        return this.commandConfigService.getCommand(id);
    }

    /**
     * Get Command configuration based on user parameters.
     *
     * @param name       Name for command (optional)
     * @param userName   The user who created the configuration (optional)
     * @param statuses   The statuses of the commands to get (optional)
     * @param tags       The set of tags you want the command for.
     * @param page       The page to start one (optional)
     * @param limit      The max number of results to return per page (optional)
     * @param descending Whether results returned in descending or ascending order (optional)
     * @param orderBys   The fields to order the results by (optional)
     * @return All the Commands matching the criteria or all if no criteria
     * @throws GenieException For any error
     */
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation(
            value = "Find commands",
            notes = "Find commands by the submitted criteria.",
            response = Command.class,
            responseContainer = "List"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    code = HttpURLConnection.HTTP_PRECON_FAILED,
                    message = "One of the statuses was invalid"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_INTERNAL_ERROR,
                    message = "Genie Server Error due to Unknown Exception"
            )
    })
    public List<Command> getCommands(
            @ApiParam(
                    value = "Name of the command."
            )
            @RequestParam(value = "name", required = false)
            final String name,
            @ApiParam(
                    value = "User who created the command."
            )
            @RequestParam(value = "userName", required = false)
            final String userName,
            @ApiParam(
                    value = "The statuses of the commands to find.",
                    allowableValues = "ACTIVE, DEPRECATED, INACTIVE"
            )
            @RequestParam(value = "status", required = false)
            final Set<String> statuses,
            @ApiParam(
                    value = "Tags for the cluster."
            )
            @RequestParam(value = "tag", required = false)
            final Set<String> tags,
            @ApiParam(
                    value = "The page to start on."
            )
            @RequestParam(value = "page", defaultValue = "0")
            final int page,
            @ApiParam(
                    value = "Max number of results per page."
            )
            @RequestParam(value = "limit", defaultValue = "1024")
            final int limit,
            @ApiParam(
                    value = "Whether results should be sorted in descending or ascending order. Defaults to descending"
            )
            @RequestParam(value = "descending", defaultValue = "true")
            final boolean descending,
            @ApiParam(
                    value = "The fields to order the results by. Must not be collection fields. Default is updated."
            )
            @RequestParam(value = "orderBy", required = false)
            final Set<String> orderBys
    ) throws GenieException {
        LOG.info(
                "Called [name | userName | status | tags | page | limit | descending | orderBys]"
        );
        LOG.info(
                name
                        + " | "
                        + userName
                        + " | "
                        + statuses
                        + " | "
                        + tags
                        + " | "
                        + page
                        + " | "
                        + limit
                        + " | "
                        + descending
                        + " | "
                        + orderBys
        );

        Set<CommandStatus> enumStatuses = null;
        if (statuses != null && !statuses.isEmpty()) {
            enumStatuses = EnumSet.noneOf(CommandStatus.class);
            for (final String status : statuses) {
                if (StringUtils.isNotBlank(status)) {
                    enumStatuses.add(CommandStatus.parse(status));
                }
            }
        }
        return this.commandConfigService.getCommands(
                name, userName, enumStatuses, tags, page, limit, descending, orderBys);
    }

    /**
     * Update command configuration.
     *
     * @param id            unique id for the configuration to update.
     * @param updateCommand the information to update the command with
     * @return The updated command
     * @throws GenieException For any error
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(
            value = "Update a command",
            notes = "Update a command from the supplied information.",
            response = Command.class
    )
    @ApiResponses(value = {
            @ApiResponse(
                    code = HttpURLConnection.HTTP_NOT_FOUND,
                    message = "Command to update not found"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_PRECON_FAILED,
                    message = "Invalid required parameter supplied"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_INTERNAL_ERROR,
                    message = "Genie Server Error due to Unknown Exception"
            )
    })
    public Command updateCommand(
            @ApiParam(
                    value = "Id of the command to update.",
                    required = true
            )
            @PathVariable("id")
            final String id,
            @ApiParam(
                    value = "The command information to update.",
                    required = true
            )
            @RequestBody
            final Command updateCommand
    ) throws GenieException {
        LOG.info("Called to update command");
        return this.commandConfigService.updateCommand(id, updateCommand);
    }

    /**
     * Delete all applications from database.
     *
     * @return All The deleted comamnd
     * @throws GenieException For any error
     */
    @RequestMapping(method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Delete all commands",
            notes = "Delete all available commands and get them back.",
            response = Command.class,
            responseContainer = "List"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    code = HttpURLConnection.HTTP_NOT_FOUND,
                    message = "Command not found"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_PRECON_FAILED,
                    message = "Invalid required parameter supplied"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_INTERNAL_ERROR,
                    message = "Genie Server Error due to Unknown Exception"
            )
    })
    public List<Command> deleteAllCommands() throws GenieException {
        LOG.info("called to delete all commands.");
        return this.commandConfigService.deleteAllCommands();
    }

    /**
     * Delete a command.
     *
     * @param id unique id for configuration to delete
     * @return The deleted configuration
     * @throws GenieException For any error
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Delete an command",
            notes = "Delete an command with the supplied id.",
            response = Command.class
    )
    @ApiResponses(value = {
            @ApiResponse(
                    code = HttpURLConnection.HTTP_NOT_FOUND,
                    message = "Command not found"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_PRECON_FAILED,
                    message = "Invalid required parameter supplied"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_INTERNAL_ERROR,
                    message = "Genie Server Error due to Unknown Exception"
            )
    })
    public Command deleteCommand(
            @ApiParam(
                    value = "Id of the command to delete.",
                    required = true
            )
            @PathVariable("id")
            final String id
    ) throws GenieException {
        LOG.info("Called to delete command with id " + id);
        return this.commandConfigService.deleteCommand(id);
    }

    /**
     * Add new configuration files to a given command.
     *
     * @param id      The id of the command to add the configuration file to. Not
     *                null/empty/blank.
     * @param configs The configuration files to add. Not null/empty/blank.
     * @return The active configurations for this command.
     * @throws GenieException For any error
     */
    @RequestMapping(value = "/{id}/configs", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(
            value = "Add new configuration files to a command",
            notes = "Add the supplied configuration files to the command with the supplied id.",
            response = String.class,
            responseContainer = "Set"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    code = HttpURLConnection.HTTP_NOT_FOUND,
                    message = "Command not found"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_PRECON_FAILED,
                    message = "Invalid required parameter supplied"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_INTERNAL_ERROR,
                    message = "Genie Server Error due to Unknown Exception"
            )
    })
    public Set<String> addConfigsForCommand(
            @ApiParam(
                    value = "Id of the command to add configuration to.",
                    required = true
            )
            @PathVariable("id")
            final String id,
            @ApiParam(
                    value = "The configuration files to add.",
                    required = true
            )
            @RequestBody
            final Set<String> configs
    ) throws GenieException {
        LOG.info("Called with id " + id + " and config " + configs);
        return this.commandConfigService.addConfigsForCommand(id, configs);
    }

    /**
     * Get all the configuration files for a given command.
     *
     * @param id The id of the command to get the configuration files for. Not
     *           NULL/empty/blank.
     * @return The active set of configuration files.
     * @throws GenieException For any error
     */
    @RequestMapping(value = "/{id}/configs", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get the configuration files for a command",
            notes = "Get the configuration files for the command with the supplied id.",
            response = String.class,
            responseContainer = "Set"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    code = HttpURLConnection.HTTP_NOT_FOUND,
                    message = "Command not found"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_PRECON_FAILED,
                    message = "Invalid required parameter supplied"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_INTERNAL_ERROR,
                    message = "Genie Server Error due to Unknown Exception"
            )
    })
    public Set<String> getConfigsForCommand(
            @ApiParam(
                    value = "Id of the command to get configurations for.",
                    required = true
            )
            @PathVariable("id")
            final String id
    ) throws GenieException {
        LOG.info("Called with id " + id);
        return this.commandConfigService.getConfigsForCommand(id);
    }

    /**
     * Update the configuration files for a given command.
     *
     * @param id      The id of the command to update the configuration files for.
     *                Not null/empty/blank.
     * @param configs The configuration files to replace existing configuration
     *                files with. Not null/empty/blank.
     * @return The new set of command configurations.
     * @throws GenieException For any error
     */
    @RequestMapping(value = "/{id}/configs", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(
            value = "Update configuration files for an command",
            notes = "Replace the existing configuration files for command with given id.",
            response = String.class,
            responseContainer = "Set"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    code = HttpURLConnection.HTTP_NOT_FOUND,
                    message = "Command not found"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_PRECON_FAILED,
                    message = "Invalid required parameter supplied"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_INTERNAL_ERROR,
                    message = "Genie Server Error due to Unknown Exception"
            )
    })
    public Set<String> updateConfigsForCommand(
            @ApiParam(
                    value = "Id of the command to update configurations for.",
                    required = true
            )
            @PathVariable("id")
            final String id,
            @ApiParam(
                    value = "The configuration files to replace existing with.",
                    required = true
            )
            @RequestBody
            final Set<String> configs
    ) throws GenieException {
        LOG.info("Called with id " + id + " and configs " + configs);
        return this.commandConfigService.updateConfigsForCommand(id, configs);
    }

    /**
     * Delete the all configuration files from a given command.
     *
     * @param id The id of the command to delete the configuration files from.
     *           Not null/empty/blank.
     * @return Empty set if successful
     * @throws GenieException For any error
     */
    @RequestMapping(value = "/{id}/configs", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Remove all configuration files from an command",
            notes = "Remove all the configuration files from the command with given id.",
            response = String.class,
            responseContainer = "Set"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    code = HttpURLConnection.HTTP_NOT_FOUND,
                    message = "Command not found"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_PRECON_FAILED,
                    message = "Invalid required parameter supplied"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_INTERNAL_ERROR,
                    message = "Genie Server Error due to Unknown Exception"
            )
    })
    public Set<String> removeAllConfigsForCommand(
            @ApiParam(
                    value = "Id of the command to delete from.",
                    required = true
            )
            @PathVariable("id")
            final String id
    ) throws GenieException {
        LOG.info("Called with id " + id);
        return this.commandConfigService.removeAllConfigsForCommand(id);
    }

    /**
     * Add new tags to a given command.
     *
     * @param id   The id of the command to add the tags to. Not
     *             null/empty/blank.
     * @param tags The tags to add. Not null/empty/blank.
     * @return The active tags for this command.
     * @throws GenieException For any error
     */
    @RequestMapping(value = "/{id}/tags", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(
            value = "Add new tags to a command",
            notes = "Add the supplied tags to the command with the supplied id.",
            response = String.class,
            responseContainer = "Set"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    code = HttpURLConnection.HTTP_NOT_FOUND,
                    message = "Command not found"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_PRECON_FAILED,
                    message = "Invalid required parameter supplied"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_INTERNAL_ERROR,
                    message = "Genie Server Error due to Unknown Exception"
            )
    })
    public Set<String> addTagsForCommand(
            @ApiParam(
                    value = "Id of the command to add configuration to.",
                    required = true
            )
            @PathVariable("id")
            final String id,
            @ApiParam(
                    value = "The tags to add.",
                    required = true
            )
            @RequestBody
            final Set<String> tags
    ) throws GenieException {
        LOG.info("Called with id " + id + " and tags " + tags);
        return this.commandConfigService.addTagsForCommand(id, tags);
    }

    /**
     * Get all the tags for a given command.
     *
     * @param id The id of the command to get the tags for. Not
     *           NULL/empty/blank.
     * @return The active set of tags.
     * @throws GenieException For any error
     */
    @RequestMapping(value = "/{id}/tags", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get the tags for a command",
            notes = "Get the tags for the command with the supplied id.",
            response = String.class,
            responseContainer = "Set"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    code = HttpURLConnection.HTTP_NOT_FOUND,
                    message = "Command not found"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_PRECON_FAILED,
                    message = "Invalid required parameter supplied"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_INTERNAL_ERROR,
                    message = "Genie Server Error due to Unknown Exception"
            )
    })
    public Set<String> getTagsForCommand(
            @ApiParam(
                    value = "Id of the command to get tags for.",
                    required = true
            )
            @PathVariable("id")
            final String id
    ) throws GenieException {
        LOG.info("Called with id " + id);
        return this.commandConfigService.getTagsForCommand(id);
    }

    /**
     * Update the tags for a given command.
     *
     * @param id   The id of the command to update the tags for.
     *             Not null/empty/blank.
     * @param tags The tags to replace existing configuration
     *             files with. Not null/empty/blank.
     * @return The new set of command tags.
     * @throws GenieException For any error
     */
    @RequestMapping(value = "/{id}/tags", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(
            value = "Update tags for a command",
            notes = "Replace the existing tags for command with given id.",
            response = String.class,
            responseContainer = "Set"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    code = HttpURLConnection.HTTP_NOT_FOUND,
                    message = "Command not found"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_PRECON_FAILED,
                    message = "Invalid required parameter supplied"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_INTERNAL_ERROR,
                    message = "Genie Server Error due to Unknown Exception"
            )
    })
    public Set<String> updateTagsForCommand(
            @ApiParam(
                    value = "Id of the command to update tags for.",
                    required = true
            )
            @PathVariable("id")
            final String id,
            @ApiParam(
                    value = "The tags to replace existing with.",
                    required = true
            )
            @RequestBody
            final Set<String> tags
    ) throws GenieException {
        LOG.info("Called with id " + id + " and tags " + tags);
        return this.commandConfigService.updateTagsForCommand(id, tags);
    }

    /**
     * Delete the all tags from a given command.
     *
     * @param id The id of the command to delete the tags from.
     *           Not null/empty/blank.
     * @return Empty set if successful
     * @throws GenieException For any error
     */
    @RequestMapping(value = "/{id}/tags", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Remove all tags from a command",
            notes = "Remove all the tags from the command with given id.  Note that the genie name space tags"
                    + "prefixed with genie.id and genie.name cannot be deleted.",
            response = String.class,
            responseContainer = "Set"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    code = HttpURLConnection.HTTP_NOT_FOUND,
                    message = "Command not found"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_PRECON_FAILED,
                    message = "Invalid required parameter supplied"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_INTERNAL_ERROR,
                    message = "Genie Server Error due to Unknown Exception"
            )
    })
    public Set<String> removeAllTagsForCommand(
            @ApiParam(
                    value = "Id of the command to delete from.",
                    required = true
            )
            @PathVariable("id")
            final String id
    ) throws GenieException {
        LOG.info("Called with id " + id);
        return this.commandConfigService.removeAllTagsForCommand(id);
    }

    /**
     * Remove an tag from a given command.
     *
     * @param id  The id of the command to delete the tag from. Not
     *            null/empty/blank.
     * @param tag The tag to remove. Not null/empty/blank.
     * @return The active set of tags for the command.
     * @throws GenieException For any error
     */
    @RequestMapping(value = "/{id}/tags/{tag}", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Remove a tag from a command",
            notes = "Remove the given tag from the command with given id.  Note that the genie name space tags"
                    + "prefixed with genie.id and genie.name cannot be deleted.",
            response = String.class,
            responseContainer = "Set"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    code = HttpURLConnection.HTTP_NOT_FOUND,
                    message = "Command not found"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_PRECON_FAILED,
                    message = "Invalid required parameter supplied"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_INTERNAL_ERROR,
                    message = "Genie Server Error due to Unknown Exception"
            )
    })
    public Set<String> removeTagForCommand(
            @ApiParam(
                    value = "Id of the command to delete from.",
                    required = true
            )
            @PathVariable("id")
            final String id,
            @ApiParam(
                    value = "The tag to remove.",
                    required = true
            )
            @PathVariable("tag")
            final String tag
    ) throws GenieException {
        LOG.info("Called with id " + id + " and tag " + tag);
        return this.commandConfigService.removeTagForCommand(id, tag);
    }

    /**
     * Set the application for the given command.
     *
     * @param id           The id of the command to add the applications to. Not
     *                     null/empty/blank.
     * @param applications The applications to set. Not null.
     * @return The active applications for this command.
     * @throws GenieException For any error
     */
    @RequestMapping(
            value = "/{id}/applications", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiOperation(
            value = "Set the applications for a command",
            notes = "Set the supplied applications to the command "
                    + "with the supplied id. Applications should already "
                    + "have been created.",
            response = Application.class,
            responseContainer = "Set"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    code = HttpURLConnection.HTTP_NOT_FOUND,
                    message = "Command not found"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_PRECON_FAILED,
                    message = "Invalid required parameter supplied"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_INTERNAL_ERROR,
                    message = "Genie Server Error due to Unknown Exception"
            )
    })
    public Set<Application> setApplicationsForCommand(
            @ApiParam(
                    value = "Id of the command to set application for.",
                    required = true
            )
            @PathVariable("id")
            final String id,
            @ApiParam(
                    value = "The applications to add.",
                    required = true
            )
            @RequestBody
            final Set<Application> applications
    ) throws GenieException {
        LOG.info("Called with id " + id + " and application " + applications);
        return this.commandConfigService.setApplicationsForCommand(id, applications);
    }

    /**
     * Get the applications configured for a given command.
     *
     * @param id The id of the command to get the application files for. Not
     *           NULL/empty/blank.
     * @return The active applications for the command.
     * @throws GenieException For any error
     */
    @RequestMapping(value = "/{id}/applications", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get the applications for a command",
            notes = "Get the applications for the command with the supplied id.",
            response = Application.class,
            responseContainer = "Set"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    code = HttpURLConnection.HTTP_NOT_FOUND,
                    message = "Command not found"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_PRECON_FAILED,
                    message = "Invalid required parameter supplied"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_INTERNAL_ERROR,
                    message = "Genie Server Error due to Unknown Exception"
            )
    })
    public Set<Application> getApplicationForCommand(
            @ApiParam(
                    value = "Id of the command to get the application for.",
                    required = true
            )
            @PathVariable("id")
            final String id
    ) throws GenieException {
        LOG.info("Called with id " + id);
        return this.commandConfigService.getApplicationsForCommand(id);
    }

    /**
     * Remove the application from a given command.
     *
     * @param id The id of the command to delete the application from. Not
     *           null/empty/blank.
     * @return The active set of applications for the command.
     * @throws GenieException For any error
     */
    @RequestMapping(value = "/{id}/applications", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Remove an applications from a command",
            notes = "Remove the applications from the command with given id.",
            response = Application.class,
            responseContainer = "Set"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    code = HttpURLConnection.HTTP_NOT_FOUND,
                    message = "Command not found"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_PRECON_FAILED,
                    message = "Invalid required parameter supplied"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_INTERNAL_ERROR,
                    message = "Genie Server Error due to Unknown Exception"
            )
    })
    public Set<Application> removeApplicationForCommand(
            @ApiParam(
                    value = "Id of the command to delete from.",
                    required = true
            )
            @PathVariable("id")
            final String id
    ) throws GenieException {
        LOG.info("Called with id '" + id + "'.");
        return this.commandConfigService.removeApplicationsForCommand(id);
    }

    /**
     * Get all the clusters this command is associated with.
     *
     * @param id       The id of the command to get the clusters for. Not
     *                 NULL/empty/blank.
     * @param statuses The statuses of the clusters to get
     * @return The list of clusters.
     * @throws GenieException For any error
     */
    @RequestMapping(value = "/{id}/clusters", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get the clusters this command is associated with",
            notes = "Get the clusters which this command exists on supports.",
            response = Cluster.class,
            responseContainer = "List"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    code = HttpURLConnection.HTTP_NOT_FOUND,
                    message = "Command not found"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_PRECON_FAILED,
                    message = "Invalid required parameter supplied"
            ),
            @ApiResponse(
                    code = HttpURLConnection.HTTP_INTERNAL_ERROR,
                    message = "Genie Server Error due to Unknown Exception"
            )
    })
    public List<Cluster> getClustersForCommand(
            @ApiParam(
                    value = "Id of the command to get the clusters for.",
                    required = true
            )
            @PathVariable("id")
            final String id,
            @ApiParam(
                    value = "Status of the cluster.",
                    allowableValues = "UP, OUT_OF_SERVICE, TERMINATED"
            )
            @RequestParam(value = "status", required = false)
            final Set<String> statuses
    ) throws GenieException {
        LOG.info("Called with id " + id + " and statuses " + statuses);

        Set<ClusterStatus> enumStatuses = null;
        if (!statuses.isEmpty()) {
            enumStatuses = EnumSet.noneOf(ClusterStatus.class);
            for (final String status : statuses) {
                if (StringUtils.isNotBlank(status)) {
                    enumStatuses.add(ClusterStatus.parse(status));
                }
            }
        }

        return this.commandConfigService.getClustersForCommand(id, enumStatuses);
    }
}