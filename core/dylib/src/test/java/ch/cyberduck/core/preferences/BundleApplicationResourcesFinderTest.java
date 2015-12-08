package ch.cyberduck.core.preferences;

/*
 * Copyright (c) 2002-2015 David Kocher. All rights reserved.
 * http://cyberduck.io/
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
 * Bug fixes, suggestions and comments should be sent to:
 * feedback@cyberduck.io
 */

import ch.cyberduck.binding.foundation.NSBundle;
import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.exception.NotfoundException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BundleApplicationResourcesFinderTest extends AbstractTestCase {

    @Test
    public void testBundle() throws Exception {
        assertNotNull(new BundleApplicationResourcesFinder().bundle());
    }

    @Test
    public void testSymbolicLink() throws Exception {
        final NSBundle bundle = new BundleApplicationResourcesFinder().bundle(NSBundle.bundleWithPath("."), new Local("/usr/bin/java"));
        assertNotNull(bundle);
        assertEquals(NSBundle.bundleWithPath("/System/Library/Frameworks/JavaVM.framework/Versions/A"), bundle);
    }

    @Test
    public void testAccessDenied() throws Exception {
        final NSBundle bundle = new BundleApplicationResourcesFinder().bundle(NSBundle.bundleWithPath("."), new Local("/usr/bin/java") {
            @Override
            public Local getSymlinkTarget() throws NotfoundException {
                throw new NotfoundException("f");
            }
        });
        assertNotNull(bundle);
        assertEquals(NSBundle.bundleWithPath("."), bundle);
    }
}