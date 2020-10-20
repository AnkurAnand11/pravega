/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.cli.admin.password;

import io.pravega.cli.admin.AdminCommand;
import io.pravega.cli.admin.CommandArgs;
import io.pravega.controller.server.security.auth.StrongPasswordProcessor;

import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

/**
 * The command takes filename and user:password:acl as argument.
 * It creates a file and writes the encrypted password to the file.
 */
public class PasswordFileCreatorCommand extends AdminCommand {

    private String toWrite;

    public PasswordFileCreatorCommand(CommandArgs args) {
        super(args);
    }

    @Override
    public void execute() {
        try {
            ensureArgCount(2);
            String targetFileName = getTargetFilename(getCommandArgs().getArgs());
            String userDetails = getUserDetails(getCommandArgs().getArgs());
            createPassword(userDetails);
            writeToFile(targetFileName, toWrite);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private String getTargetFilename(List<String> userInput) {
        return userInput.get(0);
    }

    private String getUserDetails(List<String> userInput) {
        String userDetails = userInput.get(1);

        // A sample object value comprises of  "userName:password:acl". We don't want the splits at the
        // access control entries (like "prn::/scope:testScope") in the ACL, so we restrict the splits to 3.
        if ((userDetails.split(":", 3)).length == 3) {
            return userDetails;
        } else {
            throw new IllegalArgumentException("The user detail entered is not of the format uname:pwd:acl");
        }
    }

    private void createPassword(String userDetails) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String[] lists = parseUserDetails(userDetails);
        toWrite = generatePassword(lists);
    }

    private String[] parseUserDetails(String userDetails) {
        return userDetails.split(":");
    }

    private String generatePassword(String[] lists) throws NoSuchAlgorithmException, InvalidKeySpecException {
        StrongPasswordProcessor passwordEncryptor = StrongPasswordProcessor.builder().build();
        return lists[0] + ":" + passwordEncryptor.encryptPassword(lists[1]) + ":" + lists[2] + ";";
    }

    private void writeToFile(String targetFileName, String toWrite) throws IOException {
        try (FileWriter writer = new FileWriter(targetFileName)) {
            writer.write(toWrite + "\n");
            writer.flush();
        }
    }

    public static CommandDescriptor descriptor() {
        final String component = "password";
        return new CommandDescriptor(component, "create-password-file", "Generates file with encrypted " +
                "password using filename and user:password:acl given as argument.", new ArgDescriptor("filename",
                "Name of the file generated by the command"), new ArgDescriptor("user:passwword:acl",
                "Input according to which encrypted password is generated"));
    }
}

