/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   2020-09-01 (Vyacheslav Soldatov): created
 */
package org.knime.ext.google.filehandling.drive.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;

import org.knime.ext.google.filehandling.drive.fs.FileMetadata.FileType;
import org.knime.filehandling.core.connections.base.BaseFileSystemProvider;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;
import org.knime.google.api.data.GoogleApiConnection;

/**
 * File system provider for {@link GoogleDriveFileSystem}.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class GoogleDriveFileSystemProvider extends BaseFileSystemProvider<GoogleDrivePath, GoogleDriveFileSystem> {
    /**
     * Default user drive name.
     */
    public static final String MY_DRIVE = "My Drive";

    private final GoogleDriveHelper m_helper;

    /**
     * @param connection
     *            Google API connection.
     */
    public GoogleDriveFileSystemProvider(
            final GoogleApiConnection connection) {
        m_helper = new GoogleDriveHelper(connection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SeekableByteChannel newByteChannelInternal(final GoogleDrivePath path,
            final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException {
        throw new RuntimeException("TODO");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void moveInternal(final GoogleDrivePath source, final GoogleDrivePath target, final CopyOption... options)
            throws IOException {
        throw new RuntimeException("TODO");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void copyInternal(final GoogleDrivePath source, final GoogleDrivePath target, final CopyOption... options)
            throws IOException {
        throw new RuntimeException("TODO");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected InputStream newInputStreamInternal(final GoogleDrivePath path, final OpenOption... options)
            throws IOException {
        throw new RuntimeException("TODO");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected OutputStream newOutputStreamInternal(final GoogleDrivePath path, final OpenOption... options)
            throws IOException {
        throw new RuntimeException("TODO");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Iterator<GoogleDrivePath> createPathIterator(final GoogleDrivePath dir, final Filter<? super Path> filter)
            throws IOException {
        throw new RuntimeException("TODO");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createDirectoryInternal(final GoogleDrivePath dir, final FileAttribute<?>... attrs)
            throws IOException {
        throw new RuntimeException("TODO");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean exists(final GoogleDrivePath path) throws IOException {
        try {
            checkAccessInternal(path);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected GoogleDriveFileAttributes fetchAttributesInternal(final GoogleDrivePath path, final Class<?> type)
            throws IOException {
        if (!type.isAssignableFrom(GoogleDriveFileAttributes.class)) {
            throw new UnsupportedOperationException("Unsupported attributes type: " + type.getName());
        }

        if (path.isRoot()) {
            return createRootAttributes(path);
        }

        // find nearest cached parent attributes.
        // The path segments without attributes supply into list
        GoogleDriveFileAttributes attributes = null;
        LinkedList<String> pathToExpand = new LinkedList<>();
        GoogleDrivePath current = path;

        while (attributes == null) {
            Optional<BaseFileAttributes> optional = getCachedAttributes(current);
            if (optional.isPresent()) {
                attributes = (GoogleDriveFileAttributes) optional.get();
            } else {
                pathToExpand.add(0, current.getFileName().toString());
            }

            current = current.getParent();
            if (current == null) {
                break;
            }
            if (current.isRoot()) {
                attributes = createRootAttributes(current);
            }
        }
        if (attributes == null) {
            throw new NoSuchFileException(path.toString());
        }

        // drill down from parent to child
        current = attributes.fileKey();
        Iterator<String> iter = pathToExpand.iterator();
        while (iter.hasNext()) {
            final String name = iter.next();
            final GoogleDrivePath child = current.resolve(name);

            if (current.isRoot()) {
                attributes = new GoogleDriveFileAttributes(child, new FileMetadata(m_helper.getDrive(name)));
            } else if (current.isDrive()) {
                attributes = new GoogleDriveFileAttributes(child,
                        new FileMetadata(m_helper.getFileOfDrive(attributes.getMetadata().getId(), name)));
            } else {
                attributes = new GoogleDriveFileAttributes(child,
                        new FileMetadata(m_helper.getFile(attributes.getMetadata().getId(), name)));
            }

            current = child;
        }

        return attributes;
    }

    @SuppressWarnings("resource")
    private Optional<BaseFileAttributes> getCachedAttributes(final GoogleDrivePath path) {
        return getFileSystemInternal().getCachedAttributes(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkAccessInternal(final GoogleDrivePath path, final AccessMode... modes) throws IOException {
        fetchAttributesInternal(path, GoogleDriveFileAttributes.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void deleteInternal(final GoogleDrivePath path) throws IOException {
        throw new RuntimeException("TODO");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getScheme() {
        return GoogleDriveFileSystem.FS_TYPE;
    }

    /**
     * Do close owned resources.
     */
    public void prepareClose() {
    }

    /**
     * @return the helper
     */
    public GoogleDriveHelper getHelper() {
        return m_helper;
    }

    /**
     * @param path
     *            patch.
     * @return attributes for root path.
     */
    private static GoogleDriveFileAttributes createRootAttributes(final GoogleDrivePath path) {
        return new GoogleDriveFileAttributes(path, new FileMetadata(null, FileType.ROOT));
    }
}
