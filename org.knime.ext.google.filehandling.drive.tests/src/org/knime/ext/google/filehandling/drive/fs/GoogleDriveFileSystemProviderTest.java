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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.AssertionFailedException;
import org.junit.Before;
import org.junit.Test;

import com.google.api.services.drive.model.File;

/**
 * lightweight tests without real connection to Google Drive.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class GoogleDriveFileSystemProviderTest {
    private static final String DEFAULT_DRIVE_PATH = GoogleDriveFileSystem.PATH_SEPARATOR
            + GoogleDriveFileSystemProvider.MY_DRIVE;

    private MockGoogleDriveHelper m_helper;
    private GoogleDriveFileSystemProvider m_provider;
    private MockGoogleDriveFileSystem m_fs;

    /**
     * Default constructor.
     */
    public GoogleDriveFileSystemProviderTest() {
        super();
    }

    /**
     * Initializes the test.
     *
     * @throws URISyntaxException
     */
    @Before
    public void setUp() throws URISyntaxException {
        m_fs = new MockGoogleDriveFileSystem();
        m_helper = m_fs.getHelper();
        m_provider = m_fs.provider();
    }

    /**
     * Check correct works exists method on root folder.
     *
     * @throws IOException
     */
    @Test
    public void testExistsRoot() throws IOException {
        assertTrue(m_provider.exists(createPath("/")));
    }

    /**
     * Check correct works exists method on default drive.
     *
     * @throws IOException
     */
    @Test
    public void testExistsDefaultDrive() throws IOException {
        assertTrue(m_provider.exists(createPath(DEFAULT_DRIVE_PATH)));
    }

    /**
     * Check correct works exists method on shared drive.
     *
     * @throws IOException
     */
    @Test
    public void testExistsSharedDrive() throws IOException {
        m_helper.createSharedDrive("sd").getId();

        assertFalse(m_provider.exists(createPath("/anyLeftDriveId")));
        assertTrue(m_provider.exists(createPath("/sd")));
    }

    /**
     * Check correct works exists method on file of default drive.
     *
     * @throws IOException
     */
    @Test
    public void testExistsFileOfDefaultDrive() throws IOException {
        m_helper.createFolder(null, null, "d");

        assertFalse(m_provider.exists(createPath(DEFAULT_DRIVE_PATH + "/a")));
        assertTrue(m_provider.exists(createPath(DEFAULT_DRIVE_PATH + "/d")));
    }

    /**
     * Check correct works exists method on file of shared drive.
     *
     * @throws IOException
     */
    @Test
    public void testExistsFileOfSharedDrive() throws IOException {
        String driveId = m_helper.createSharedDrive("sd").getId();
        m_helper.createFolder(driveId, null, "d");

        assertFalse(m_provider.exists(createPath("/sd/a")));
        assertTrue(m_provider.exists(createPath("/sd/d")));
    }

    /**
     * Check correct fetch attributes of root folder.
     *
     * @throws IOException
     */
    @Test
    public void testFetchAttributesInternalOrRoot() throws IOException {
        assertNotNull(m_provider.fetchAttributesInternal(createPath("/"), GoogleDriveFileAttributes.class));
    }

    /**
     * Check correct fetch attributes of root folder.
     *
     * @throws IOException
     */
    @Test
    public void testFetchAttributesInternalOfDefaultDrive() throws IOException {
        assertNotNull(
                m_provider.fetchAttributesInternal(createPath(DEFAULT_DRIVE_PATH), GoogleDriveFileAttributes.class));
    }

    /**
     * Check correct fetch attributes of shared folder.
     *
     * @throws IOException
     */
    @Test
    public void testFetchAttributesInternalOfSharedDrive() throws IOException {
        m_helper.createSharedDrive("sd");

        try {
            m_provider.fetchAttributesInternal(createPath("/anyNotExisting"), GoogleDriveFileAttributes.class);
            throw new AssertionFailedException("NoSuchFileException should be thrown");
        } catch (NoSuchFileException ex) { // NOSONAR exception is correct
            // good
        }

        assertNotNull(m_provider.fetchAttributesInternal(createPath("/sd"), GoogleDriveFileAttributes.class));
    }

    /**
     * Check correct fetch attributes of file of default drive.
     *
     * @throws IOException
     */
    @Test
    public void testFetchAttributesInternalOfFileOfDefaultDrive() throws IOException {
        m_helper.createFolder(null, null, "d");

        try {
            m_provider.fetchAttributesInternal(createPath(DEFAULT_DRIVE_PATH + "/a"), GoogleDriveFileAttributes.class);
            throw new AssertionFailedException("NoSuchFileException should be thrown");
        } catch (NoSuchFileException ex) { // NOSONAR exception is correct
            // good
        }
        assertNotNull(m_provider.fetchAttributesInternal(createPath(DEFAULT_DRIVE_PATH + "/d"),
                GoogleDriveFileAttributes.class));
    }

    /**
     * Check correct fetch attributes of file of shared drive.
     *
     * @throws IOException
     */
    @Test
    public void testFetchAttributesInternalOfFileOfSharedDrive() throws IOException {
        String driveId = m_helper.createSharedDrive("sd").getId();
        m_helper.createFolder(driveId, null, "d");

        try {
            m_provider.fetchAttributesInternal(createPath("/sd/a"), GoogleDriveFileAttributes.class);
            throw new AssertionFailedException("NoSuchFileException should be thrown");
        } catch (NoSuchFileException ex) { // NOSONAR exception is correct
            // good
        }
        assertNotNull(m_provider.fetchAttributesInternal(createPath("/sd/d"), GoogleDriveFileAttributes.class));
    }

    /**
     * Tests file names corrected when list directory.
     *
     * @throws IOException
     */
    @Test
    public void testListDuplicateFiles() throws IOException {
        final String parentId = m_helper.createFolder(null, null, "parent").getId();

        File folder1 = m_helper.createFolder(null, parentId, "d");
        File folder2 = m_helper.createFolder(null, parentId, "d");

        List<GoogleDrivePath> files = listFolder(createPath("/My Drive/parent"));
        assertEquals("/My Drive/parent/d (" + folder1.getId() + ")", files.get(0).toString()); // NOSONAR
        assertEquals("/My Drive/parent/d (" + folder2.getId() + ")", files.get(1).toString());
    }

    /**
     * Tests case when one from files in folder already has a name which should be
     * generated as synthetic name.
     *
     * @throws IOException
     */
    @Test
    public void testRealFileExistsWithSumulatedName() throws IOException {
        final String parentId = m_helper.createFolder(null, null, "parent").getId();

        File folder1 = m_helper.createFolder(null, parentId, "d");
        File folder2 = m_helper.createFolder(null, parentId, "d");
        m_helper.createFolder(null, parentId, "d (1)");

        List<GoogleDrivePath> files = listFolder(createPath("/My Drive/parent"));
        assertEquals("/My Drive/parent/d (" + folder1.getId() + ")", files.get(0).toString());
        assertEquals("/My Drive/parent/d (" + folder2.getId() + ")", files.get(1).toString());
        assertEquals("/My Drive/parent/d (1)", files.get(2).toString());
    }

    /**
     * Tests synthetic path allows to correct access file.
     *
     * @throws IOException
     */
    @Test
    public void testAccessToFileBySynteticName() throws IOException {
        final String parentId = m_helper.createFolder(null, null, "parent").getId();

        File f1 = m_helper.createFolder(null, parentId, "d");
        File f2 = m_helper.createFolder(null, parentId, "d");

        assertEquals(f1.getId(),
                m_provider.fetchAttributesInternal(createPath("/My Drive/parent/d (" + f1.getId() + ")")).getMetadata()
                        .getId());
        assertEquals(f2.getId(),
                m_provider.fetchAttributesInternal(createPath("/My Drive/parent/d (" + f2.getId() + ")")).getMetadata()
                        .getId());
    }

    /**
     * Tests return correct schema.
     */
    @Test
    public void testGetScheme() {
        assertEquals(GoogleDriveFileSystem.FS_TYPE, m_provider.getScheme());
    }

    private List<GoogleDrivePath> listFolder(final GoogleDrivePath path) throws IOException {
        final List<GoogleDrivePath> files = new LinkedList<>();
        try (DirectoryStream<Path> stream = m_provider.newDirectoryStream(path, null)) {
            stream.forEach(p -> files.add((GoogleDrivePath) p));
        }
        return files;
    }

    private GoogleDrivePath createPath(final String pathString) {
        return m_fs.getPath(pathString);
    }
}
