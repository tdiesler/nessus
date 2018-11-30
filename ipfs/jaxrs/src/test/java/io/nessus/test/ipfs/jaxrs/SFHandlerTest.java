package io.nessus.test.ipfs.jaxrs;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import io.nessus.ipfs.FHandle;
import io.nessus.ipfs.jaxrs.SFHandle;
import io.nessus.utils.FileUtils;

public class SFHandlerTest extends AbstractJAXRSTest {

    @Test
    public void fileTree() throws Exception {

        Path srcPath = Paths.get("src/test/resources/contentA");
        Path dstPath = cntmgr.getPlainPath(addrBob).resolve("contentA");
        if (!dstPath.toFile().exists())
            FileUtils.recursiveCopy(srcPath, dstPath);
        
        FHandle fh = cntmgr.findLocalContent(addrBob, Paths.get("contentA"));
        SFHandle sfh = new SFHandle(fh);
        LOG.info("{}", sfh.toString(true));
    }
}
