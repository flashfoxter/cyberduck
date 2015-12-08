package ch.cyberduck.core.irods;

/*
 * Copyright (c) 2002-2015 David Kocher. All rights reserved.
 * http://cyberduck.ch/
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
 *
 * Bug fixes, suggestions and comments should be sent to feedback@cyberduck.ch
 */

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledCancelCallback;
import ch.cyberduck.core.DisabledConnectionCallback;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.DisabledLoginCallback;
import ch.cyberduck.core.DisabledPasswordStore;
import ch.cyberduck.core.DisabledTranscriptListener;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Profile;
import ch.cyberduck.core.ProfileReaderFactory;
import ch.cyberduck.core.ProtocolFactory;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.io.BandwidthThrottle;
import ch.cyberduck.core.io.Checksum;
import ch.cyberduck.core.io.DisabledStreamListener;
import ch.cyberduck.core.io.MD5ChecksumCompute;
import ch.cyberduck.core.shared.DefaultHomeFinderService;
import ch.cyberduck.core.transfer.TransferStatus;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * @version $Id$
 */
public class IRODSUploadFeatureTest extends AbstractTestCase {

    @BeforeClass
    public static void protocol() {
        ProtocolFactory.register(new IRODSProtocol());
    }

    @Test
    @Ignore
    public void testAppend() throws Exception {
        final Profile profile = ProfileReaderFactory.get().read(
                new Local("../profiles/iRODS (iPlant Collaborative).cyberduckprofile"));
        final Host host = new Host(profile, profile.getDefaultHostname(), new Credentials(
                properties.getProperty("irods.key"), properties.getProperty("irods.secret")
        ));

        final IRODSSession session = new IRODSSession(host);
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        final Local local = new Local(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        final byte[] content = new byte[32770];
        new Random().nextBytes(content);
        final OutputStream out = local.getOutputStream(false);
        IOUtils.write(content, out);
        IOUtils.closeQuietly(out);
        final Checksum checksumPart1;
        final Checksum checksumPart2;
        final Path test = new Path(new DefaultHomeFinderService(session).find(), UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));
        {
            final TransferStatus status = new TransferStatus().length(content.length / 2);
            checksumPart1 = new IRODSUploadFeature(session).upload(
                    test, local, new BandwidthThrottle(BandwidthThrottle.UNLIMITED), new DisabledStreamListener(),
                    status,
                    new DisabledConnectionCallback());
            assertEquals(content.length / 2, status.getOffset());
        }
        {
            final TransferStatus status = new TransferStatus().length(content.length / 2).skip(content.length / 2).append(true);
            checksumPart2 = new IRODSUploadFeature(session).upload(
                    test, local, new BandwidthThrottle(BandwidthThrottle.UNLIMITED), new DisabledStreamListener(),
                    status,
                    new DisabledConnectionCallback());
            assertEquals(content.length / 2, status.getOffset());
        }
        assertNotEquals(checksumPart1, checksumPart2);
        final byte[] buffer = new byte[content.length];
        final InputStream in = new IRODSReadFeature(session).read(test, new TransferStatus().length(content.length));
        IOUtils.readFully(in, buffer);
        IOUtils.closeQuietly(in);
        assertArrayEquals(content, buffer);
        new IRODSDeleteFeature(session).delete(Collections.singletonList(test), new DisabledLoginCallback(), new Delete.Callback() {
            @Override
            public void delete(final Path file) {
            }
        });
        session.close();
    }

    @Test
    public void testWrite() throws Exception {
        final Profile profile = ProfileReaderFactory.get().read(
                new Local("../profiles/iRODS (iPlant Collaborative).cyberduckprofile"));
        final Host host = new Host(profile, profile.getDefaultHostname(), new Credentials(
                properties.getProperty("irods.key"), properties.getProperty("irods.secret")
        ));

        final IRODSSession session = new IRODSSession(host);
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        final Local local = new Local(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        final byte[] content = new byte[32770];
        new Random().nextBytes(content);
        final OutputStream out = local.getOutputStream(false);
        IOUtils.write(content, out);
        IOUtils.closeQuietly(out);
        final Checksum checksum;
        final Path test = new Path(new DefaultHomeFinderService(session).find(), UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));
        final TransferStatus status = new TransferStatus().length(content.length);
        checksum = new IRODSUploadFeature(session).upload(
                test, local, new BandwidthThrottle(BandwidthThrottle.UNLIMITED), new DisabledStreamListener(),
                status,
                new DisabledConnectionCallback());
        assertTrue(status.isComplete());
        assertEquals(content.length, status.getOffset());
        assertEquals(checksum, new MD5ChecksumCompute().compute(new FileInputStream(local.getAbsolute())));
        final byte[] buffer = new byte[content.length];
        final InputStream in = new IRODSReadFeature(session).read(test, new TransferStatus().length(content.length));
        IOUtils.readFully(in, buffer);
        IOUtils.closeQuietly(in);
        assertArrayEquals(content, buffer);
        new IRODSDeleteFeature(session).delete(Collections.singletonList(test), new DisabledLoginCallback(), new Delete.Callback() {
            @Override
            public void delete(final Path file) {
            }
        });
        session.close();
    }

    @Test
    public void testInterruptStatus() throws Exception {
        final Profile profile = ProfileReaderFactory.get().read(
                new Local("../profiles/iRODS (iPlant Collaborative).cyberduckprofile"));
        final Host host = new Host(profile, profile.getDefaultHostname(), new Credentials(
                properties.getProperty("irods.key"), properties.getProperty("irods.secret")
        ));

        final IRODSSession session = new IRODSSession(host);
        session.open(new DisabledHostKeyCallback(), new DisabledTranscriptListener());
        session.login(new DisabledPasswordStore(), new DisabledLoginCallback(), new DisabledCancelCallback());
        final Local local = new Local(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        final byte[] content = new byte[32770];
        new Random().nextBytes(content);
        final OutputStream out = local.getOutputStream(false);
        IOUtils.write(content, out);
        IOUtils.closeQuietly(out);
        final Path test = new Path(new DefaultHomeFinderService(session).find(), UUID.randomUUID().toString(), EnumSet.of(Path.Type.file));
        final TransferStatus status = new TransferStatus().length(content.length);
        final Checksum checksum = new IRODSUploadFeature(session).upload(
                test, local, new BandwidthThrottle(BandwidthThrottle.UNLIMITED), new DisabledStreamListener() {
                    @Override
                    public void sent(final long bytes) {
                        super.sent(bytes);
                        status.setCanceled();
                    }
                },
                status,
                new DisabledConnectionCallback());
        assertTrue(status.isCanceled());
        assertFalse(status.isComplete());
        session.close();
    }
}