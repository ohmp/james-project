

package org.apache.james.sieverepository.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.filesystem.api.FileUrl;
import org.apache.james.sieverepository.api.SieveRepository;
import org.apache.james.sieverepository.lib.AbstractSieveRepositoryTest;
import org.junit.After;
import org.junit.Before;

public class SieveFileRepositoryTest extends AbstractSieveRepositoryTest {

    private static final FileUrl SIEVE_ROOT = FileUrl.relativeFile("sieve");

    private FileSystem fileSystem;

    @Override
    @Before
    public void setUp() throws Exception {
        this.fileSystem = new FileSystem() {
            @Override
            public File getBasedir() {
                return new File(System.getProperty("java.io.tmpdir"));
            }
            
            @Override
            public InputStream getResource(FileUrl url) throws IOException {
                return new FileInputStream(getFile(url));
            }
            
            @Override
            public File getFile(FileUrl fileURL) {
                return new File(getBasedir(), fileURL.toRelativeFilePath());
            }
        };
        super.setUp();
    }

    @Override
    protected SieveRepository createSieveRepository() throws Exception {
        return new SieveFileRepository(fileSystem);
    }

    @After
    public void tearDown() throws Exception {
        File root = fileSystem.getFile(SIEVE_ROOT);
        // Remove files from the previous test, if any
        if (root.exists()) {
            FileUtils.forceDelete(root);
        }
    }
}
