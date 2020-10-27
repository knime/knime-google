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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link GoogleDrivePathIterator}
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class GoogleDrivePathIteratorTest {
    private MockGoogleDriveFileSystem m_fs;

    /**
     * Default constructor.
     */
    public GoogleDrivePathIteratorTest() {
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
    }

    /**
     * Tests iterator created with empty content.
     *
     * @throws IOException
     */
    @Test
    public void testEmptyPageableUsed() throws IOException {
        GoogleDrivePathIterator emptyIterator = createIterator(m_fs.getPath("/"), null);
        List<GoogleDrivePath> result = readAll(emptyIterator);

        assertEquals(0, result.size());
    }

    /**
     * Test iterator created with several pages content and reads them to end.
     *
     * @throws IOException
     */
    @Test
    public void testPageableWithSeveralPagesUsed() throws IOException {
        GoogleDrivePathIterator iter = createIterator(m_fs.getPath("/"), null, "a", "b", "c", "d");

        List<GoogleDrivePath> result = readAll(iter);
        assertEquals(4, result.size());

        assertEquals("a", result.get(0).getFileName().toString());
        assertEquals("b", result.get(1).getFileName().toString());
        assertEquals("c", result.get(2).getFileName().toString());
        assertEquals("d", result.get(3).getFileName().toString());
    }

    /**
     * Test file filter.
     *
     * @throws IOException
     */
    @Test
    public void testFilter() throws IOException {
        Filter<? super Path> filter = p -> p.getFileName().toString().equals("a");
        GoogleDrivePathIterator iter = createIterator(m_fs.getPath("/"), filter, "a", "b", "a", "d");

        List<GoogleDrivePath> result = readAll(iter);
        assertEquals(2, result.size());

        assertEquals("a", result.get(0).getFileName().toString());
        assertEquals("a", result.get(1).getFileName().toString());
    }

    private GoogleDrivePathIterator createIterator(final GoogleDrivePath dir, final Filter<? super Path> filter,
            final String... fileNames) throws IOException {
        List<GoogleDrivePath> files = new LinkedList<>();
        for (String name : fileNames) {
            files.add(m_fs.getPath(name));
        }
        return new GoogleDrivePathIterator(dir, files, filter);
    }

    private static List<GoogleDrivePath> readAll(final GoogleDrivePathIterator pathIterator) {
        List<GoogleDrivePath> result = new LinkedList<>();
        while (pathIterator.hasNext()) {
            result.add(pathIterator.next());
        }
        return result;
    }
}
