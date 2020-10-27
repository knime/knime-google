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
 *   2020-09-16 (Vyacheslav Soldatov): created
 */
package org.knime.ext.google.filehandling.drive.fs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.Objects;
import com.google.api.services.drive.model.Drive;
import com.google.api.services.drive.model.File;

/**
 * Replaces calls to real Google API by calls to local memory cache. Not all
 * methods of {@link GoogleDriveHelper}
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class MockGoogleDriveHelper extends GoogleDriveHelper {
    private final List<Drive> m_drives = new LinkedList<>();
    private final List<File> m_files = new LinkedList<>();

    private long m_lastId = 11111111111l;

    /**
     * Default constructor.
     */
    public MockGoogleDriveHelper() {
        createDrive(null, GoogleDriveFileSystemProvider.MY_DRIVE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File createFile(final String driveId, final String folderId, final String name,
            final AbstractInputStreamContent content) throws IOException {
        File file = createFileDefault(driveId, folderId, name);
        file.setMimeType(content.getType());
        file.set("unit-test-file-content", readFully(content));
        return file;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File createFolder(final String driveId, final String parentId, final String name) throws IOException {
        File file = createFileDefault(driveId, parentId, name);
        file.setMimeType(MIME_TYPE_FOLDER);
        return file;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteFile(final String id) throws IOException {
        Set<String> ids = new HashSet<>();
        ids.add(id);
        deleteDeeply(ids);
    }

    private void deleteDeeply(final Set<String> ids) {
        if (ids.isEmpty()) {
            return;
        }

        Set<String> children = new HashSet<>();
        Iterator<File> iter = m_files.iterator();
        while (iter.hasNext()) {
            File next = iter.next();
            if (ids.contains(next.getId())) {
                iter.remove();
            } else if (next.getParents() != null) {
                for (String parentId : next.getParents()) {
                    children.add(parentId);
                }
            } else {
                // nothing (just for sonar fix)
            }
        }

        deleteDeeply(children);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Drive> getDrives(final String name, final String driveId) throws IOException {
        List<Drive> drives = new LinkedList<>();

        for (Drive drive : m_drives) {
            if (drive.getName().equals(name) || (driveId != null && driveId.equals(drive.getId()))) {
                drives.add(drive);
            }
        }

        if (drives.isEmpty()) {
            throw new NoSuchFileException(name);
        }

        return drives;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<File> getFilesByNameOrId(final String driveId, final String parentFolderId, final String name,
            final String fileId) throws IOException {
        List<File> files = new LinkedList<>();
        for (File file : m_files) {
            if (Objects.equal(file.getDriveId(), driveId) && containsParent(file, parentFolderId)
                    && (file.getName().equals(name) || file.getId().equals(fileId))) {
                files.add(file);
            }
        }

        if (files.isEmpty()) {
            throw new NoSuchFileException(name);
        }

        return files;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<File> getFilesOfDriveByNameOrId(final String driveId, final String name, final String additionalName)
            throws IOException {
        return getFilesByNameOrId(driveId, driveId, name, additionalName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<File> listDrive(final String id) throws IOException {
        List<File> files = new LinkedList<>();
        for (File file : m_files) {
            if (Objects.equal(file.getDriveId(), id)
                    && (containsParent(file, id) || file.getParents().isEmpty())) {
                files.add(file);
            }
        }
        return files;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<File> listFolder(final String driveId, final String folderId) throws IOException {
        List<File> files = new LinkedList<>();
        for (File file : m_files) {
            if (Objects.equal(file.getDriveId(), driveId) && (containsParent(file, folderId))) {
                files.add(file);
            }
        }
        return files;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Drive> listSharedDrives() throws IOException {
        List<Drive> drives = new LinkedList<>(m_drives);

        Iterator<Drive> iter = drives.iterator();
        while (iter.hasNext()) {
            if (iter.next().getId() == null) {
                iter.remove();
                break;
            }
        }
        return drives;
    }

    /**
     * @param name
     *            shared drive name.
     * @return shared drive instance.
     */
    public Drive createSharedDrive(final String name) {
        m_lastId++;
        return createDrive("drv:" + m_lastId, name);
    }

    private Drive createDrive(final String id, final String name) {
        Drive drive = new Drive();
        drive.setName(name);
        drive.setId(id);

        DateTime currentDate = new DateTime(System.currentTimeMillis());
        drive.setCreatedTime(currentDate);
        m_drives.add(drive);
        return drive;
    }

    /**
     * @param driveId
     * @param parentId
     * @param name
     * @return
     */
    private File createFileDefault(final String driveId, final String parentId, final String name) {
        File file = new File();
        file.setName(name);
        file.setDriveId(driveId);
        if (file.getParents() == null) {
            file.setParents(new LinkedList<>());
        }
        if (parentId != null) {
            file.getParents().add(parentId);
        } else if (driveId != null) { // NOSONAR correct. The drive ID can be null if default drive
            file.getParents().add(driveId);
        }
        file.setId(nextId());

        DateTime currentDate = new DateTime(System.currentTimeMillis());
        file.setCreatedTime(currentDate);
        m_files.add(file);
        return file;
    }

    /**
     * @return next unique ID.
     */
    private String nextId() {
        m_lastId++;
        return "fid:" + m_lastId;
    }

    private static byte[] readFully(final AbstractInputStreamContent content) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = content.getInputStream()) {
            byte[] buff = new byte[256];
            int len;
            while ((len = in.read(buff)) > -1) {
                out.write(buff, 0, len);
            }
        }
        return out.toByteArray();
    }

    private static boolean containsParent(final File file, final String folderId) {
        return containsEquals(file.getParents(), folderId);
    }

    private static boolean containsEquals(final List<String> elements, final String element) {
        if (elements == null) {
            return false;
        }
        if (elements.isEmpty() && element == null) {
            return true;
        }
        for (String e : elements) {
            if (Objects.equal(e, element)) {
                return true;
            }
        }
        return false;
    }
}
