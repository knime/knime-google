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
 *   2020-09-15 (Vyacheslav Soldatov): created
 */
package org.knime.ext.google.filehandling.drive.fs;

import java.io.IOException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.knime.filehandling.core.connections.base.BasePathIterator;

import com.google.api.services.drive.model.Drive;
import com.google.api.services.drive.model.File;

/**
 * Google Drive implementation for Path iterator.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public final class GoogleDrivePathIterator extends BasePathIterator<GoogleDrivePath> {
    /**
     * @param filter
     *            path filter.
     * @param dir
     *            path to list.
     * @throws IOException
     */
    public GoogleDrivePathIterator(final GoogleDrivePath dir,
            final Filter<? super Path> filter) throws IOException {
        super(dir, filter);

        @SuppressWarnings("resource")
        final GoogleDriveFileSystemProvider provider = dir.getFileSystem().provider();

        final List<GoogleDrivePath> files;
        if (dir.isRoot()) {
            files = listRootFolder(dir, provider);
        } else {
            files = listDriveOrFolder(dir, provider);
        }

        setFirstPage(files.iterator());
    }

    /**
     * @param dir
     *            drive or folder.
     * @param provider
     * @return files with folder content for next use it in path iterator.
     * @throws IOException
     */
    @SuppressWarnings("resource")
    private static List<GoogleDrivePath> listDriveOrFolder(final GoogleDrivePath dir,
            final GoogleDriveFileSystemProvider provider) throws IOException {

        final GoogleDriveFileAttributes attr = provider.readAttributes(dir);
        final FileMetadata meta = attr.getMetadata();

        final List<File> files;
        if (dir.isDrive()) {
            files = dir.getFileSystem().provider().getHelper().listDrive(meta.getId());
        } else {
            files = dir.getFileSystem().provider().getHelper().listFolder(meta.getDriveId(), meta.getId());
        }

        correctFileNameDuplicates(files);
        return createPathsAndCacheAttributes(dir, filesToMetadata(files));
    }

    /**
     * @param dir
     *            drive or folder.
     * @param provider
     * @return files with root file content for next use it in path iterator.
     * @throws IOException
     */
    private static List<GoogleDrivePath> listRootFolder(final GoogleDrivePath dir,
            final GoogleDriveFileSystemProvider provider) throws IOException {

        final List<Drive> sharedDrives = provider.getHelper().listSharedDrives();
        correctDriveNameDuplicates(sharedDrives);

        // shared drives is retrieved without pagination therefore can be
        // cached immediately.
        final List<GoogleDrivePath> files = createPathsAndCacheAttributes(dir, drivesToMetadata(sharedDrives));
        // add 'My Drive'
        files.add(0, dir.resolve(GoogleDriveFileSystemProvider.MY_DRIVE));
        return files;
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
        final Map<String, Integer> numItemsWithName = new HashMap<>();
        for (T obj : objects) {
            final String name = accessor.getName(obj);
            Integer numItems = numItemsWithName.computeIfAbsent(name, key -> 0);
            numItemsWithName.put(name, numItems + 1);
        }

        for (T obj : objects) {
            String name = accessor.getName(obj);

            // correct names with adding suffix
            if (numItemsWithName.get(name) > 1) {
                name += GoogleDriveFileSystemProvider.SYNTHETIC_SUFFIX_START + accessor.getId(obj) + ")";
                accessor.setCorrectedName(obj, name);
            }
        }
    }

    @SuppressWarnings("resource")
    private static List<GoogleDrivePath> createPathsAndCacheAttributes(final GoogleDrivePath parent,
            final List<FileMetadata> childMetas) {

        List<GoogleDrivePath> files = new LinkedList<>();
        for (FileMetadata meta : childMetas) {
            // create path
            GoogleDrivePath path = parent.resolve(meta.getName());
            files.add(path);

            // cache attributes
            parent.getFileSystem().addToAttributeCache(path, new GoogleDriveFileAttributes(path, meta));
        }
        return files;
    }

    /**
     * @param drives
     *            Google Drive drives.
     * @return unified file metadata from drives.
     */
    private static List<FileMetadata> drivesToMetadata(final List<Drive> drives) {
        return drives.stream() //
                .map(FileMetadata::new) //
                .collect(Collectors.toList());
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
}
