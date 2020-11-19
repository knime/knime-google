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
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.knime.ext.google.filehandling.drive.fs.FileMetadata.FileType;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.base.BaseFileSystemProvider;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;
import org.knime.google.api.data.GoogleApiConnection;

import com.google.api.services.drive.model.Drive;
import com.google.api.services.drive.model.File;

/**
 * File system provider for {@link GoogleDriveFileSystem}.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class GoogleDriveFileSystemProvider extends BaseFileSystemProvider<GoogleDrivePath, GoogleDriveFileSystem> {

    private static final Pattern GOOGLE_DOC_MIME_TYPE = Pattern.compile("application/vnd\\.google-apps\\..+");

    /**
     * Default user drive name.
     */
    public static final String MY_DRIVE = "My Drive";

    /**
     * Start of synthetic suffix for duplicate names.
     */
    public static final String SYNTHETIC_SUFFIX_START = " (";

    private final GoogleDriveHelper m_helper;

    private final Map<Path, GoogleDriveFileAttributes> m_drives = new HashMap<>();

    /**
     * @param connection
     *            Google API connection.
     * @param config
     *            connection configuration.
     */
    public GoogleDriveFileSystemProvider(final GoogleApiConnection connection,
            final GoogleDriveConnectionConfiguration config) {
        this(new GoogleDriveHelper(connection, config));
    }

    private synchronized GoogleDriveFileAttributes getDriveAttrs(final GoogleDrivePath drivePath) throws IOException {
        if (m_drives.isEmpty()) {
            fillDriveMap(drivePath);
        }

        if (m_drives.containsKey(drivePath)) {
            return m_drives.get(drivePath);
        } else {
            throw new NoSuchFileException(drivePath.toString());
        }
    }

    private void fillDriveMap(final GoogleDrivePath drivePath) throws IOException {
        try (DirectoryStream<Path> drives = Files.newDirectoryStream(drivePath.getParent())) {
            for (Path drive : drives) {
                final GoogleDrivePath gdrive = (GoogleDrivePath) drive;

                Optional<BaseFileAttributes> attrs = getCachedAttributes(gdrive);
                if (attrs.isPresent()) {
                    m_drives.put(gdrive, (GoogleDriveFileAttributes) attrs.get());
                } else {
                    m_drives.put(gdrive, new GoogleDriveFileAttributes(gdrive,
                            new FileMetadata(getDrive(drive.getFileName().toString()))));
                }
            }
        }
    }

    /**
     * This constructor is for unit tests.
     *
     * @param helper
     *            Google Drive Helper.
     */
    protected GoogleDriveFileSystemProvider(final GoogleDriveHelper helper) {
        m_helper = helper;
    }

    @Override
    protected SeekableByteChannel newByteChannelInternal(final GoogleDrivePath path,
            final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException {
        return new GoogleDriveFileSeekableByteChannel(path, options);
    }

    @SuppressWarnings("resource")
    @Override
    protected void moveInternal(final GoogleDrivePath source, final GoogleDrivePath target, final CopyOption... options)
            throws IOException {

        // validation
        if (target.isRoot()) {
            throw new AccessDeniedException(target.toString());
        }

        if (source.isRoot() || source.isDrive()) {
            throw new AccessDeniedException(source.toString());
        }

        final GoogleDriveFileAttributes sourceAttrs = readAttributes(source);
        final GoogleDriveFileAttributes targetParentAttrs = readAttributes(target.getParent());
        GoogleDriveFileAttributes targetAttrs = null;
        try {
            targetAttrs = readAttributes(target);
        } catch (NoSuchFileException e) { // NOSONAR ignore, just needed for later
        }

        File movedFile = null;

        if (targetAttrs == null) {
            movedFile = moveToNewFile(target, sourceAttrs, targetParentAttrs);
        } else if (targetAttrs.isRegularFile() || (targetAttrs.isDirectory() && !target.isDrive())) {
            movedFile = moveToFileOrDir(target, sourceAttrs, targetParentAttrs, targetAttrs);
        } else if (target.isDrive()) {
            moveToDrive(source, target, sourceAttrs);
        } else {
            throw new IOException(String.format("Cannot replace %s with a file", target.toString()));
        }

        if (movedFile != null) {
            cacheAttributes(target, new GoogleDriveFileAttributes(target, new FileMetadata(movedFile)));
        }

        getFileSystemInternal().removeFromAttributeCacheDeep(source);
    }

    private void moveToDrive(final GoogleDrivePath source, final GoogleDrivePath target,
            final GoogleDriveFileAttributes sourceAttrs) throws IOException {
        if (sourceAttrs.isRegularFile()) {
            throw new IOException(String.format("Cannot replace drive %s with a file", target.toString()));
        } else if (FSFiles.isNonEmptyDirectory(source)) {
            throw new IOException(
                    String.format("Cannot replace drive %s with non-empty directory", target.toString()));
        } else {
            // source is an empty directory and the target drive exists already (and is
            // empty)
            m_helper.deleteFile(sourceAttrs.getMetadata().getId());
        }
    }

    private File moveToFileOrDir(final GoogleDrivePath target, final GoogleDriveFileAttributes sourceAttrs,
            final GoogleDriveFileAttributes targetParentAttrs, final GoogleDriveFileAttributes targetAttrs)
            throws IOException {
        File movedFile;
        movedFile = m_helper.move(sourceAttrs.getMetadata().getId(), //
                targetParentAttrs.getMetadata().getId(), //
                target.getFileName().toString());
        m_helper.deleteFile(targetAttrs.getMetadata().getId());
        return movedFile;
    }

    private File moveToNewFile(final GoogleDrivePath target, final GoogleDriveFileAttributes sourceAttrs,
            final GoogleDriveFileAttributes targetParentAttrs) throws IOException {
        File movedFile;
        if (target.isDrive()) {
            throw new IOException("Cannot create drive " + target.toString());
        }
        // target does not exist
        movedFile = m_helper.move(sourceAttrs.getMetadata().getId(), //
                targetParentAttrs.getMetadata().getId(), //
                target.getFileName().toString());
        return movedFile;
    }

    @SuppressWarnings("resource")
    @Override
    protected void copyInternal(final GoogleDrivePath source, final GoogleDrivePath target, final CopyOption... options)
            throws IOException {

        // validation
        if (target.isRoot()) {
            throw new AccessDeniedException(target.toString());
        }

        GoogleDriveFileAttributes targetAttrs = null;
        try {
            targetAttrs = readAttributes(target);
        } catch (NoSuchFileException e) { // NOSONAR ignore, just needed for later
        }

        // copy
        GoogleDriveFileAttributes sourceAttrs = readAttributes(source);
        GoogleDriveFileAttributes targetParentAttrs = readAttributes(target.getParent());

        if (!sourceAttrs.isDirectory()) {
            // Google drive allows multiple files with same name be the child of same
            // parent. Therefore copy of file not conflicts with already existing target
            // file, but target file should be deleted after copy finished.
            m_helper.copy(sourceAttrs.getMetadata().getId(), //
                    targetParentAttrs.getMetadata().getId(), //
                    target.getFileName().toString());
        } else if (targetAttrs == null) {
            createDirectory(target);
        }

        if (targetAttrs != null && !target.isDrive()) {
            m_helper.deleteFile(targetAttrs.getMetadata().getId());
        }
        getFileSystemInternal().removeFromAttributeCacheDeep(target);
    }

    @Override
    protected InputStream newInputStreamInternal(final GoogleDrivePath path, final OpenOption... options)
            throws IOException {

        final GoogleDriveFileAttributes attrs = readAttributes(path);

        if (GOOGLE_DOC_MIME_TYPE.matcher(attrs.getMetadata().getMimeType()).matches()) {
            throw new IOException("Cannot read Google workspace document. Please use the Google API Connector nodes.");
        }

        return getHelper().readFile(attrs.getMetadata().getId());
    }

    @SuppressWarnings("resource")
    @Override
    protected OutputStream newOutputStreamInternal(final GoogleDrivePath path, final OpenOption... options)
            throws IOException {
        final Set<OpenOption> opts = new HashSet<>(Arrays.asList(options));
        return Channels.newOutputStream(newByteChannel(path, opts));
    }

    @Override
    protected Iterator<GoogleDrivePath> createPathIterator(final GoogleDrivePath dir, final Filter<? super Path> filter)
            throws IOException {

        return new GoogleDrivePathIterator(dir, filter);
    }

    @Override
    protected void createDirectoryInternal(final GoogleDrivePath dir, final FileAttribute<?>... attrs)
            throws IOException {

        if (dir.isRoot() || dir.isDrive()) {
            throw new AccessDeniedException(dir.toString());
        }

        final FileMetadata parentMeta = readAttributes(dir.getParent()).getMetadata();
        final File folder = m_helper.createFolder(parentMeta.getDriveId(), parentMeta.getId(),
                dir.getFileName().toString());
        cacheAttributes(dir, new GoogleDriveFileAttributes(dir, new FileMetadata(folder)));
    }

    @Override
    protected GoogleDriveFileAttributes fetchAttributesInternal(final GoogleDrivePath path, final Class<?> type)
            throws IOException {

        if (path.isRoot()) {
            return createRootAttributes(path);
        } else if (path.isDrive()) {
            return getDriveAttrs(path);
        } else {
            // find nearest cached parent attributes.
            // The path segments without attributes supply into list
            final GoogleDriveFileAttributes attributes = getNearestAvailableCachedAttributes(path);

            // drill down from parent to child
            final List<String> pathToExpand = getRemainingPathSegments(attributes.fileKey(), path);

            return getChildAttributesRecursively(attributes, pathToExpand);
        }
    }

    @SuppressWarnings("resource") // file system implementation is closeable by another way
    private Optional<BaseFileAttributes> getCachedAttributes(final GoogleDrivePath path) {
        return getFileSystemInternal().getCachedAttributes(path);
    }

    @SuppressWarnings("resource") // file system implementation is closeable by another way
    private void cacheAttributes(final GoogleDrivePath path, final GoogleDriveFileAttributes attr) {
        getFileSystemInternal().addToAttributeCache(path, attr);
    }

    @Override
    protected void deleteInternal(final GoogleDrivePath path) throws IOException {
        if (path.isRoot() || path.isDrive()) {
            throw new AccessDeniedException(path.toString());
        }

        final FileMetadata meta = readAttributes(path).getMetadata();
        m_helper.deleteFile(meta.getId());
    }

    GoogleDriveFileAttributes readAttributes(final GoogleDrivePath path) throws IOException {
        return (GoogleDriveFileAttributes) readAttributes(path, BasicFileAttributes.class);
    }

    /**
     * Google drive has not possibility to just request attributes by path.
     * Therefore need to request attributes from any known root to required child
     * sequentially.
     *
     * @param rootAttributes
     *            root attributes.
     * @param pathToExpand
     *            path to expand
     * @return child attributes.
     * @throws IOException
     */
    private GoogleDriveFileAttributes getChildAttributesRecursively(final GoogleDriveFileAttributes rootAttributes,
            final List<String> pathToExpand) throws IOException {

        GoogleDriveFileAttributes attributes = rootAttributes;
        GoogleDrivePath current = attributes.fileKey();

        for (String childName : pathToExpand) {
            final GoogleDrivePath child = current.resolve(childName);

            if (current.isRoot()) {
                attributes = new GoogleDriveFileAttributes(child, new FileMetadata(getDrive(childName)));
            } else {
                FileMetadata meta = attributes.getMetadata();
                if (current.isDrive()) {
                    attributes = new GoogleDriveFileAttributes(child,
                            new FileMetadata(getFileOfDrive(meta.getId(), childName)));
                } else {
                    attributes = new GoogleDriveFileAttributes(child,
                            new FileMetadata(getFile(meta.getDriveId(), meta.getId(), childName)));
                }
            }

            current = child;
            cacheAttributes(current, attributes);
        }

        return attributes;
    }

    /**
     * @param pathAncestor
     *            parent path.
     * @param path
     *            child path.
     * @return difference between parent path and child path as a path segments.
     */
    private static List<String> getRemainingPathSegments(final GoogleDrivePath pathAncestor, final GoogleDrivePath path) {
        List<String> remaining = new LinkedList<>();
        GoogleDrivePath current = path;

        while (!current.equals(pathAncestor)) {
            remaining.add(0, current.getFileName().toString());
            current = current.getParent();
        }

        return remaining;
    }

    /**
     * @param path
     *            current path.
     * @return nearest found cached attributes from given element to top
     *         sequentially.
     */
    private GoogleDriveFileAttributes getNearestAvailableCachedAttributes(final GoogleDrivePath path) {
        // find nearest cached parent attributes.
        // The path segments without attributes supply into list
        GoogleDriveFileAttributes attributes = null;
        GoogleDrivePath current = path;

        while (attributes == null) {
            final Optional<BaseFileAttributes> optional = getCachedAttributes(current);
            if (optional.isPresent()) {
                attributes = (GoogleDriveFileAttributes) optional.get();
            } else {
                current = current.getParent();
                if (current.isRoot()) {
                    attributes = createRootAttributes(current);
                }
            }
        }

        return attributes;
    }

    /**
     * @return the Google Drive helper
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

    // support of files with equal names in context of one parent
    /**
     * @param originName
     *            drive name.
     * @return drive with given name or null if 'My Drive'
     * @throws IOException
     */
    private Drive getDrive(final String originName) throws IOException {
        if (MY_DRIVE.equals(originName)) {
            return null;
        }

        String name = decodeForwardSlashes(originName);
        String fileId = getIdFromSuffixOrNull(name);
        return getBestDrive(m_helper.getDrives(name, fileId), name);
    }

    /**
     * @param driveId
     *            drive ID.
     * @param parentId
     *            parent ID.
     * @param originName
     *            file name.
     * @return
     * @throws IOException
     */
    private File getFile(final String driveId, final String parentId, final String originName) throws IOException {
        String name = decodeForwardSlashes(originName);
        String fileId = getIdFromSuffixOrNull(name);
        return getBestFile(m_helper.getFilesByNameOrId(driveId, parentId, name, fileId), name);
    }

    /**
     * @param driveId
     *            drive ID.
     * @param originName
     *            file name.
     * @return
     * @throws IOException
     */
    private File getFileOfDrive(final String driveId, final String originName) throws IOException {
        String name = decodeForwardSlashes(originName);
        String fileId = getIdFromSuffixOrNull(name);
        return getBestFile(m_helper.getFilesOfDriveByNameOrId(driveId, name, fileId), name);
    }

    private static File getBestFile(final List<File> files, final String name) throws IOException {
        File file = getBestItem(files, name, NameIdAccessor.FILE);
        if (file == null) {
            throw new IOException("Unable to select file with name '" + name + "' from list of files");
        }
        return file;
    }

    private static Drive getBestDrive(final List<Drive> drives, final String name) throws IOException {
        Drive drive = getBestItem(drives, name, NameIdAccessor.DRIVE);
        if (drive == null) {
            throw new IOException("Unable to select drive with name '" + name + "' from list of drives");
        }
        return drive;
    }

    private static <T> T getBestItem(final List<T> items, final String name, final NameIdAccessor<T> accessor) {
        // first of all attempt to select by equals name
        for (T item : items) {
            if (accessor.getName(item).equals(name)) {
                return item;
            }
        }

        // second attempt to get ID and name from synthetic name
        String id = getIdFromSuffixOrNull(name);
        if (id != null) {
            String realName = name.substring(0, name.lastIndexOf(SYNTHETIC_SUFFIX_START));

            for (T item : items) {
                if (accessor.getId(item).equals(id) && accessor.getName(item).equals(realName)) {
                    return item;
                }
            }
        }

        return null;
    }

    /**
     * @param name
     *            file name.
     * @return name without synthetic suffix or null if has not synthetic suffix.
     */
    private static String getIdFromSuffixOrNull(final String name) {
        if (!name.endsWith(")")) {
            // has not synthetic suffix
            return null;
        }

        int suffixStart = name.lastIndexOf(SYNTHETIC_SUFFIX_START);
        if (suffixStart < 0) {
            // has not synthetic suffix
            return null;
        }

        return name.substring(suffixStart + SYNTHETIC_SUFFIX_START.length(), name.length() - 1);
    }

    @Override
    protected void checkAccessInternal(final GoogleDrivePath path, final AccessMode... modes) throws IOException {
        // do nothing
    }

    /**
     * @param originName
     *            origin name.
     * @return decoded name.
     */
    static String decodeForwardSlashes(final String originName) {
        return originName.replace("$_$", "/");
    }

    /**
     * @param originName
     *            origin name.
     * @return decoded name.
     */
    static String encodeForwardSlashes(final String originName) {
        return originName.replace("/", "$_$");
    }
}
