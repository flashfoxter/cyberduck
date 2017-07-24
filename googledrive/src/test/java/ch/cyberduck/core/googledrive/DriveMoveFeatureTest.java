package ch.cyberduck.core.googledrive;

/*
 * Copyright (c) 2002-2016 iterate GmbH. All rights reserved.
 * https://cyberduck.io/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

import ch.cyberduck.core.AlphanumericRandomStringService;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledCancelCallback;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.DisabledLoginCallback;
import ch.cyberduck.core.DisabledPasswordStore;
import ch.cyberduck.core.DisabledProgressListener;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginConnectionService;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathCache;
import ch.cyberduck.core.Scheme;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.features.Find;
import ch.cyberduck.core.shared.DefaultFindFeature;
import ch.cyberduck.core.ssl.DefaultX509KeyManager;
import ch.cyberduck.core.ssl.DefaultX509TrustManager;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.test.IntegrationTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;
import java.util.EnumSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category(IntegrationTest.class)
public class DriveMoveFeatureTest {

    @Test
    public void testMoveFile() throws Exception {
        final Host host = new Host(new DriveProtocol(), "www.googleapis.com", new Credentials());
        final DriveSession session = new DriveSession(host, new DefaultX509TrustManager(), new DefaultX509KeyManager());
        new LoginConnectionService(new DisabledLoginCallback(), new DisabledHostKeyCallback(),
                new DisabledPasswordStore() {
                    @Override
                    public String getPassword(Scheme scheme, int port, String hostname, String user) {
                        if(user.equals("Google Drive OAuth2 Access Token")) {
                            return System.getProperties().getProperty("googledrive.accesstoken");
                        }
                        if(user.equals("Google Drive OAuth2 Refresh Token")) {
                            return System.getProperties().getProperty("googledrive.refreshtoken");
                        }
                        return null;
                    }
                }, new DisabledProgressListener()
        ).connect(session, PathCache.empty(), new DisabledCancelCallback());
        final Path test = new Path(new DriveHomeFinderService(session).find(), new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.file));
        new DriveTouchFeature(session).touch(test, new TransferStatus());
        final Path folder = new Path(new DriveHomeFinderService(session).find(), new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.directory));
        new DriveDirectoryFeature(session).mkdir(folder, null, new TransferStatus());
        final Path target = new Path(folder, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.file));
        new DriveMoveFeature(session).move(test, target, new TransferStatus(), new Delete.DisabledCallback());
        final Find find = new DefaultFindFeature(session);
        assertFalse(find.find(test));
        assertTrue(find.find(target));
        new DriveDeleteFeature(session).delete(Arrays.asList(target, folder), new DisabledLoginCallback(), new Delete.DisabledCallback());
    }

    @Test
    public void testMoveDirectory() throws Exception {
        final Host host = new Host(new DriveProtocol(), "www.googleapis.com", new Credentials());
        final DriveSession session = new DriveSession(host, new DefaultX509TrustManager(), new DefaultX509KeyManager());
        new LoginConnectionService(new DisabledLoginCallback(), new DisabledHostKeyCallback(),
                new DisabledPasswordStore() {
                    @Override
                    public String getPassword(Scheme scheme, int port, String hostname, String user) {
                        if(user.equals("Google Drive OAuth2 Access Token")) {
                            return System.getProperties().getProperty("googledrive.accesstoken");
                        }
                        if(user.equals("Google Drive OAuth2 Refresh Token")) {
                            return System.getProperties().getProperty("googledrive.refreshtoken");
                        }
                        return null;
                    }

                    @Override
                    public String getPassword(String hostname, String user) {
                        return super.getPassword(hostname, user);
                    }
                }, new DisabledProgressListener()
        ).connect(session, PathCache.empty(), new DisabledCancelCallback());
        final Path sourceDirectory = new Path(new DriveHomeFinderService(session).find(), new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.directory));
        final Path targetDirectory = new Path(new DriveHomeFinderService(session).find(), new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.directory));
        new DriveDirectoryFeature(session).mkdir(sourceDirectory, null, new TransferStatus());
        final Path sourceFile = new Path(sourceDirectory, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.file));
        new DriveTouchFeature(session).touch(sourceFile, new TransferStatus());
        final Path targetFile = new Path(targetDirectory, sourceFile.getName(), EnumSet.of(Path.Type.file));
        new DriveMoveFeature(session).move(sourceDirectory, targetDirectory, new TransferStatus(), new Delete.DisabledCallback());
        final Find find = new DefaultFindFeature(session);
        assertFalse(find.find(sourceDirectory));
        assertTrue(find.find(targetDirectory));
        new DriveDeleteFeature(session).delete(Arrays.asList(targetFile, targetDirectory), new DisabledLoginCallback(), new Delete.DisabledCallback());
    }
}
