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
import java.util.concurrent.TimeUnit;

import com.google.api.client.util.DateTime;
import com.google.api.services.drive.model.Drive;
import com.google.api.services.drive.model.File;

/**
 * Contains metadata of Google Drive file or drive
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
    private final String m_name;
    private final String m_mimeType;

    /**
     * @param id
     *            Google element ID if presented.
     * @param type
     *            file type.
     */
    public FileMetadata(final String id, final FileType type) {
        super();

        m_name = null;
        m_mimeType = null;
        m_id = id;
        m_type = type;
        m_driveId = null;
        m_createdTime = null;
        m_lastModifiedTime = null;
        m_lastAccessTime = null;
        m_size = 0l;
    }

    /**
     * @param file
     *            Google Drive file.
     */
    public FileMetadata(final File file) {
        m_id = file.getId();
        m_name = file.getName();
        m_mimeType = file.getMimeType();
        m_type = GoogleDriveHelper.MIME_TYPE_FOLDER.equals(file.getMimeType()) ? FileType.FOLDER : FileType.FILE;
        m_driveId = file.getDriveId();

        FileTime lastModifiedTime = getTime(file.getModifiedTime());
        m_lastModifiedTime = lastModifiedTime;
        m_lastAccessTime = lastModifiedTime;
        m_createdTime = getTime(file.getCreatedTime());

        m_size = file.getSize() == null ? 0 : file.getSize();
    }

    /**
     * @param drive
     *            shared drive or null for `My Drive`
     */
    public FileMetadata(final Drive drive) {
        m_id = drive == null ? null : drive.getId();
        m_mimeType = null;
        m_name = drive == null ? GoogleDriveFileSystemProvider.MY_DRIVE : drive.getName();
        m_type = drive == null ? FileType.MY_DRIVE : FileType.SHARED_DRIVE;
        m_driveId = m_id;

        m_lastModifiedTime = null;
        m_lastAccessTime = null;
        m_createdTime = drive == null ? null : getTime(drive.getCreatedTime());

        m_size = 0;
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
     * @return file (drive) name.
     */
    public String getName() {
        return m_name;
    }

    /**
     * @return mime type of file.
     */
    public String getMimeType() {
        return m_mimeType;
    }
}
