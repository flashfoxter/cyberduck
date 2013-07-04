package ch.cyberduck.core.exception;

/*
 * Copyright (c) 2002-2013 David Kocher. All rights reserved.
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

import ch.cyberduck.core.ftp.FTPException;

import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;

/**
 * @version $Id:$
 */
public class FTPExceptionMappingService extends AbstractIOExceptionMappingService<IOException> {

    @Override
    public BackgroundException map(final IOException e) {
        final StringBuilder buffer = new StringBuilder();
        this.append(buffer, e.getMessage());
        if(e instanceof FTPException) {
            if(((FTPException) e).getCode() == FTPReply.INSUFFICIENT_STORAGE) {
                return new QuotaException(buffer.toString(), e);
            }
            if(((FTPException) e).getCode() == FTPReply.STORAGE_ALLOCATION_EXCEEDED) {
                return new QuotaException(buffer.toString(), e);
            }
            if(((FTPException) e).getCode() == FTPReply.NOT_LOGGED_IN) {
                return new LoginFailureException(buffer.toString(), e);
            }
            if(((FTPException) e).getCode() == FTPReply.FILE_UNAVAILABLE) {
                return new NotfoundException(buffer.toString(), e);
            }
        }
        return this.wrap(e, buffer);
    }
}
