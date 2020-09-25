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
 *   2020-09-14 (Vyacheslav Soldatov): created
 */
package org.knime.ext.google.filehandling.drive.fs;

import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.api.client.util.DateTime;
import com.google.api.services.drive.model.Drive;
import com.google.api.services.drive.model.File;

/**
 * Contains metadata of google file or drive
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class FileMetadata {
    /**
     * File type.
     */
    public enum FileType {
        /**
         * Root file.
         */
        ROOT,
        /**
         * Default user drive with name 'My Drive'
         */
        MY_DRIVE,
        /**
         * Shared drive.
         */
        SHARED_DRIVE,
        /**
         * Folder.
         */
        FOLDER,
        /**
         * File.
         */
        FILE
    }

    private final String m_id;
    private final FileType m_type;
    private final String m_driveId;
    private final FileTime m_createdTime;
    private final FileTime m_lastModifiedTime;
    private final FileTime m_lastAccessTime;
    private final long m_size;
    private final UserPrincipal m_owner;
    private final GroupPrincipal m_group;
    private final Set<PosixFilePermission> m_permissions;
    private String m_name;

    /**
     * @param id
     *            Google element ID if presented.
     * @param type
     *            file type.
     */
    public FileMetadata(final String id, final FileType type) {
        super();

        m_id = id;
        m_type = type;
        m_driveId = null;
        m_createdTime = null;
        m_lastModifiedTime = null;
        m_lastAccessTime = null;
        m_size = 0l;
        m_owner = null;
        m_group = null;
        m_permissions = new HashSet<>();
    }

    /**
     * @param file
     *            GoogleDrive file.
     */
    public FileMetadata(final File file) {
        m_id = file.getId();
        m_name = file.getName();
        m_type = GoogleDriveHelper.MIME_TYPE_FOLDER.equals(file.getMimeType()) ? FileType.FOLDER : FileType.FILE;
        m_driveId = file.getDriveId();

        FileTime lastModifiedTime = getTime(file.getModifiedTime());
        m_lastModifiedTime = lastModifiedTime;
        m_lastAccessTime = lastModifiedTime;
        m_createdTime = getTime(file.getCreatedTime());

        m_size = file.getSize() == null ? 0 : file.getSize();
        m_owner = getOwner(file);
        m_group = getGroup(file);
        m_permissions = getPermissions(file);
    }

    /**
     * @param drive
     *            shared drive or null for `My Drive`
     */
    public FileMetadata(final Drive drive) {
        m_id = drive == null ? null : drive.getId();
        m_name = drive == null ? GoogleDriveFileSystemProvider.MY_DRIVE : drive.getName();
        m_type = drive == null ? FileType.MY_DRIVE : FileType.SHARED_DRIVE;
        m_driveId = m_id;

        m_lastModifiedTime = null;
        m_lastAccessTime = null;
        m_createdTime = drive == null ? null : getTime(drive.getCreatedTime());

        m_size = 0;
        m_owner = getOwner(drive);
        m_group = getGroup(drive);
        m_permissions = getPermissions(drive);
    }

    /**
     * @param dateTime
     *            date time to convert.
     * @return tile time.
     */
    private static FileTime getTime(final DateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        return FileTime.fromMillis(dateTime.getValue() + TimeUnit.MINUTES.toMillis(dateTime.getTimeZoneShift()));
    }

    /**
     * @param file
     *            Google file.
     * @return Posix file permissions.
     */
    private static Set<PosixFilePermission> getPermissions(final File file) {
        return new HashSet<>();
    }

    /**
     * @param drive
     *            Google shared drive.
     * @return Posix file permissions.
     */
    private static Set<PosixFilePermission> getPermissions(final Drive drive) {
        return new HashSet<>();
    }

    /**
     * @param file
     *            Google file.
     * @return group principal.
     */
    private static GroupPrincipal getGroup(final File file) {
        // TODO implement
        return null;
    }

    /**
     * @param drive
     *            Google shared drive.
     * @return group principal.
     */
    private static GroupPrincipal getGroup(final Drive drive) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param file
     *            Google file
     * @return owner principal.
     */
    private static UserPrincipal getOwner(final File file) {
        // TODO implement
        return null;
    }

    /**
     * @param drive
     *            Google shared drive.
     * @return user principal.
     */
    private static UserPrincipal getOwner(final Drive drive) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @return file ID.
     */
    public String getId() {
        return m_id;
    }

    /**
     * @return file type.
     */
    public FileType getType() {
        return m_type;
    }

    /**
     * @return drive ID for files and folders.
     */
    public String getDriveId() {
        return m_driveId;
    }

    /**
     * @return file creation time.
     */
    public FileTime getCreatedTime() {
        return m_createdTime;
    }

    /**
     * @return file last modified time.
     */
    public FileTime getLastModifiedTime() {
        return m_lastModifiedTime;
    }

    /**
     * @return file last accessed time.
     */
    public FileTime getLastAccessTime() {
        return m_lastAccessTime;
    }

    /**
     * @return file size.
     */
    public long getSize() {
        return m_size;
    }

    /**
     * @return file owner.
     */
    public UserPrincipal getOwner() {
        return m_owner;
    }

    /**
     * @return file group.
     */
    public GroupPrincipal getGroup() {
        return m_group;
    }

    /**
     * @return positx permissions for the file.
     */
    public Set<PosixFilePermission> getPermissions() {
        return m_permissions;
    }

    /**
     * @return file (drive) name.
     */
    public String getName() {
        return m_name;
    }
}
