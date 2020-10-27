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
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.OpenOption;
import java.nio.file.Path;
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

import org.knime.ext.google.filehandling.drive.fs.FileMetadata.FileType;
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
    /**
     * Default user drive name.
     */
    public static final String MY_DRIVE = "My Drive";
    /**
     * Start of synthetic suffix for duplicate names.
     */
    private static final String SYNTHETIC_SUFFIX_START = " (";

    private final GoogleDriveHelper m_helper;

    /**
     * @param connection
     *            Google API connection.
     */
    public GoogleDriveFileSystemProvider(final GoogleApiConnection connection) {
        this(new GoogleDriveHelper(connection));
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected SeekableByteChannel newByteChannelInternal(final GoogleDrivePath path,
            final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException {
        return new GoodleDriveFileSeekableByteChannel(path, options);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("resource")
    @Override
    protected void moveInternal(final GoogleDrivePath source, final GoogleDrivePath target, final CopyOption... options)
            throws IOException {
        // validation
        if (source.isRoot()) {
            throw new AccessDeniedException("Root folder can't be moved");
        }
        if (source.isDrive()) {
            throw new AccessDeniedException("Drive can't be moved");
        }

        GoogleDrivePath targetParent = target.getParent();
        if (targetParent.isRoot()) {
            throw new AccessDeniedException("Can't move into root folder just into drives");
        }

        FileMetadata existingTargetMeta = null;
        if (exists(target)) {
            existingTargetMeta = fetchAttributesInternal(target).getMetadata();
            if (existingTargetMeta.getType() == FileType.FOLDER && !isEmptyFolder(existingTargetMeta)) {
                throw new DirectoryNotEmptyException(target.toString());
            }
        }

        // moving
        FileMetadata sourceMeta = fetchAttributesInternal(source).getMetadata();
        FileMetadata targetParentMeta = fetchAttributesInternal(targetParent).getMetadata();

        File file = m_helper.move(sourceMeta.getId(), targetParentMeta.getId(), target.getFileName().toString());

        // if not any exceptions thrown should clear the cache deeply
        getFileSystemInternal().removeFromAttributeCacheDeep(source);

        // cache new attributes for moved file
        cacheAttributes(target, new GoogleDriveFileAttributes(target, new FileMetadata(file)));

        // Google drive allows for many files with same name be the child of same
        // parent.
        // Therefore moving of file not conflicts with already existing target file, but
        // target file should be deleted after moving finished.
        if (existingTargetMeta != null) {
            m_helper.deleteFile(existingTargetMeta.getId());
            getFileSystemInternal().removeFromAttributeCacheDeep(target);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("resource")
    @Override
    protected void copyInternal(final GoogleDrivePath source, final GoogleDrivePath target, final CopyOption... options)
            throws IOException {
        // validation
        if (source.isRoot()) {
            throw new AccessDeniedException("Root folder can't be copied");
        }
        if (source.isDrive()) {
            throw new AccessDeniedException("Drive can't be copied");
        }

        GoogleDrivePath targetParent = target.getParent();
        if (targetParent.isRoot()) {
            throw new AccessDeniedException("Can't copy into root folder just into drives");
        }

        FileMetadata targetMeta = null;
        if (exists(target)) {
            targetMeta = fetchAttributesInternal(target).getMetadata();
            if (targetMeta.getType() == FileType.FOLDER && !isEmptyFolder(targetMeta)) {
                throw new DirectoryNotEmptyException(target.toString());
            }
        }

        // copy
        FileMetadata sourceMeta = fetchAttributesInternal(source).getMetadata();
        FileMetadata targetParentMeta = fetchAttributesInternal(targetParent).getMetadata();

        if (targetMeta != null) { // file already exists
            if (targetMeta.getType() == FileType.FILE) {
                m_helper.copy(sourceMeta.getId(), targetParentMeta.getId(), target.getFileName().toString());

                // Google drive allows to many files with same name be the child of same
                // parent.
                // Therefore copy of file not conflicts with already existing target file, but
                // target file should be deleted after copy finished.
                m_helper.deleteFile(targetMeta.getId());
                getFileSystemInternal().removeFromAttributeCacheDeep(target);
            } else {
                // copy to container (folder or drive)
                m_helper.copy(sourceMeta.getId(), targetMeta.getId(), source.getFileName().toString());
            }
        } else {
            // target is interpreted as an end target
            m_helper.copy(sourceMeta.getId(), targetParentMeta.getId(), target.getFileName().toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("resource")
    @Override
    protected InputStream newInputStreamInternal(final GoogleDrivePath path, final OpenOption... options)
            throws IOException {
        final Set<OpenOption> opts = new HashSet<>(Arrays.asList(options));
        return Channels.newInputStream(newByteChannel(path, opts));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("resource")
    @Override
    protected OutputStream newOutputStreamInternal(final GoogleDrivePath path, final OpenOption... options)
            throws IOException {
        final Set<OpenOption> opts = new HashSet<>(Arrays.asList(options));
        return Channels.newOutputStream(newByteChannel(path, opts));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Iterator<GoogleDrivePath> createPathIterator(final GoogleDrivePath dir, final Filter<? super Path> filter)
            throws IOException {
        List<GoogleDrivePath> files;
        if (dir.isRoot()) {
            files = listRootFolder(dir);
        } else {
            files = listDriveOrFolder(dir);
        }

        return new GoogleDrivePathIterator(dir, files, filter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createDirectoryInternal(final GoogleDrivePath dir, final FileAttribute<?>... attrs)
            throws IOException {
        FileMetadata meta = fetchAttributesInternal(dir.getParent()).getMetadata();
        File folder = m_helper.createFolder(meta.getDriveId(), meta.getId(), dir.getFileName().toString());
        cacheAttributes(dir, new GoogleDriveFileAttributes(dir, new FileMetadata(folder)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean exists(final GoogleDrivePath path) throws IOException {
        try {
            checkAccessInternal(path);
            return true;
        } catch (IOException ex) { // NOSONAR I/O exception is correct when remote file is not exist
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
        GoogleDriveFileAttributes attributes = getNearestAvailableCachedAttributes(path);

        // drill down from parent to child
        GoogleDrivePath current = attributes.fileKey();
        List<String> pathToExpand = getRemainingPathSegments(current, path);

        return getChildAttributesRecursively(attributes, pathToExpand);
    }

    @SuppressWarnings("resource") // file system implementation is closeable by another way
    private Optional<BaseFileAttributes> getCachedAttributes(final GoogleDrivePath path) {
        return getFileSystemInternal().getCachedAttributes(path);
    }

    @SuppressWarnings("resource") // file system implementation is closeable by another way
    private void cacheAttributes(final GoogleDrivePath path, final GoogleDriveFileAttributes attr) {
        getFileSystemInternal().addToAttributeCache(path, attr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkAccessInternal(final GoogleDrivePath path, final AccessMode... modes) throws IOException {
        fetchAttributesInternal(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void deleteInternal(final GoogleDrivePath path) throws IOException {
        if (path.isRoot()) {
            throw new AccessDeniedException("Can't delete root");
        }
        if (path.isDrive()) {
            throw new AccessDeniedException("Can't delete drive");
        }

        FileMetadata meta = fetchAttributesInternal(path).getMetadata();
        if (meta.getType() == FileType.FOLDER && !isEmptyFolder(meta)) {
            throw new DirectoryNotEmptyException(path.toString());
        }

        m_helper.deleteFile(meta.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getScheme() {
        return GoogleDriveFileSystem.FS_TYPE;
    }

    GoogleDriveFileAttributes fetchAttributesInternal(final GoogleDrivePath path) throws IOException {
        return fetchAttributesInternal(path, GoogleDriveFileAttributes.class);
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

        Iterator<String> iter = pathToExpand.iterator();
        while (iter.hasNext()) {
            final String name = iter.next();
            final GoogleDrivePath child = current.resolve(name);

            if (current.isRoot()) {
                attributes = new GoogleDriveFileAttributes(child, new FileMetadata(getDrive(name)));
            } else {
                FileMetadata meta = attributes.getMetadata();
                if (current.isDrive()) {
                    attributes = new GoogleDriveFileAttributes(child,
                            new FileMetadata(getFileOfDrive(meta.getId(), name)));
                } else {
                    attributes = new GoogleDriveFileAttributes(child,
                            new FileMetadata(getFile(meta.getDriveId(), meta.getId(), name)));
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
            Optional<BaseFileAttributes> optional = getCachedAttributes(current);
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

    private boolean isEmptyFolder(final FileMetadata meta) throws IOException {
        return m_helper.listFolder(meta.getDriveId(), meta.getId()).isEmpty();
    }

    /**
     * @param dir
     *            drive or folder.
     * @return files with folder content for next use it in path iterator.
     * @throws IOException
     */
    private List<GoogleDrivePath> listDriveOrFolder(final GoogleDrivePath dir) throws IOException {
        GoogleDriveFileAttributes attr = fetchAttributesInternal(dir);

        FileMetadata meta = attr.getMetadata();
        List<File> files;
        if (dir.isDrive()) {
            files = m_helper.listDrive(meta.getId());
        } else {
            files = m_helper.listFolder(meta.getDriveId(), meta.getId());
        }

        correctFileNameDuplicates(files);
        return createPathsAndCacheAttributes(dir, filesToMetadata(files));
    }

    /**
     * @param dir
     *            drive or folder.
     * @return files with root file content for next use it in path iterator.
     * @throws IOException
     */
    private List<GoogleDrivePath> listRootFolder(final GoogleDrivePath dir) throws IOException {
        final List<Drive> sharedDrives = m_helper.listSharedDrives();
        correctDriveNameDuplicates(sharedDrives);

        final List<FileMetadata> metas = drivesToMetadata(sharedDrives);
        // shared drives is retrieved without pagination therefore can be
        // cached immediately.
        List<GoogleDrivePath> files = createPathsAndCacheAttributes(dir, metas);
        // add 'My Drive'
        files.add(0, dir.resolve(MY_DRIVE));
        return files;
    }

    private List<GoogleDrivePath> createPathsAndCacheAttributes(final GoogleDrivePath parent, final List<FileMetadata> childMetas) {
        List<GoogleDrivePath> files = new LinkedList<>();
        for (FileMetadata meta : childMetas) {
            // create path
            GoogleDrivePath path = parent.resolve(meta.getName());
            files.add(path);

            // cache attributes
            cacheAttributes(path, new GoogleDriveFileAttributes(path, meta));
        }
        return files;
    }

    /**
     * @param drives
     *            Google Drive drives.
     * @return unified file metadata from drives.
     */
    private static List<FileMetadata> drivesToMetadata(final List<Drive> drives) {
        List<FileMetadata> meta = new LinkedList<>();
        for (Drive drive : drives) {
            meta.add(new FileMetadata(drive));
        }
        return meta;
    }

    /**
     * @param drives
     *            Google Drive drives.
     * @return unified file metadata from files.
     */
    static List<FileMetadata> filesToMetadata(final List<File> files) {
        List<FileMetadata> meta = new LinkedList<>();
        for (File file : files) {
            meta.add(new FileMetadata(file));
        }
        return meta;
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
     * @param name
     *            drive name.
     * @return drive with given name or null if 'My Drive'
     * @throws IOException
     */
    private Drive getDrive(final String name) throws IOException {
        if (MY_DRIVE.equals(name)) {
            return null;
        }

        String fileId = getIdFromSuffixOrNull(name);
        return getBestDrive(m_helper.getDrives(name, fileId), name);
    }

    /**
     * @param driveId
     *            drive ID.
     * @param parentId
     *            parent ID.
     * @param name
     *            file name.
     * @return
     * @throws IOException
     */
    private File getFile(final String driveId, final String parentId, final String name) throws IOException {
        String fileId = getIdFromSuffixOrNull(name);
        return getBestFile(m_helper.getFilesByNameOrId(driveId, parentId, name, fileId), name);
    }

    /**
     * @param driveId
     *            drive ID.
     * @param name
     *            file name.
     * @return
     * @throws IOException
     */
    private File getFileOfDrive(final String driveId, final String name) throws IOException {
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

    private static void correctDriveNameDuplicates(final List<Drive> sharedDrives) {
        correctNameDuplicates(sharedDrives, NameIdAccessor.DRIVE);
    }

    private static void correctFileNameDuplicates(final List<File> files) {
        correctNameDuplicates(files, NameIdAccessor.FILE);
    }

    private static <T> void correctNameDuplicates(final List<T> objects, final NameIdAccessor<T> accessor) {
        // create map where key is name and value is
        // the number of items with given name.
        Map<String, Integer> numItemsWithName = new HashMap<>();
        for (T obj : objects) {
            final String name = accessor.getName(obj);
            Integer numItems = numItemsWithName.computeIfAbsent(name, key -> 0);
            numItemsWithName.put(name, numItems + 1);
        }

        for (T obj : objects) {
            String name = accessor.getName(obj);

            // correct names with adding suffix
            if (numItemsWithName.get(name) > 1) {
                name += SYNTHETIC_SUFFIX_START + accessor.getId(obj) + ")";
                accessor.setCorrectedName(obj, name);
            }
        }
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
}
