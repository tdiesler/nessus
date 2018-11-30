package io.nessus.test.ipfs;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.ipfs.FHandle;
import io.nessus.ipfs.FHandle.FHBuilder;
import io.nessus.utils.FileUtils;

public class FHandleTest extends AbstractWorkflowTest {

    @Test
    public void treeTest() throws Exception {
        
        // Build the tree of plain file handles
        Path srcPath = Paths.get("src/test/resources/contentA");
        Path dstPath = cntmgr.getPlainPath(addrBob).resolve("contentA");
        
        if (!dstPath.toFile().exists())
            FileUtils.recursiveCopy(srcPath, dstPath);
        
        FHandle fhandleA = cntmgr.buildTreeFromPath(addrBob, Paths.get("contentA"));
        
        LOG.info(fhandleA.toString(true));
        Assert.assertEquals("contentA", fhandleA.getPath().toString());
        Assert.assertEquals(3, fhandleA.getChildren().size());
        
        // Change root properties & maintain children
        
        FHandle fhandleB = new FHBuilder(fhandleA).cid("fhandleB").build();
        LOG.info(fhandleB.toString(true));
        Assert.assertEquals("fhandleB", fhandleB.getCid());
        Assert.assertEquals("contentA", fhandleB.getPath().toString());
        Assert.assertEquals(3, fhandleB.getChildren().size());
        FHandle fhChild = fhandleB.getChildren().get(1);
        Assert.assertEquals("fhandleB/subB", fhChild.getCid());
        String fhandleBstr = fhandleB.toString(true);
        
        // Modify a subtree element
        
        Path childPath = fhandleA.getPath().resolve("subB/subC/file02.txt");
        FHandle fhandleC = new FHBuilder(fhandleA).findChild(childPath).cid("fhandleC").build();
        LOG.info(fhandleC.toString(true));
        Assert.assertEquals("fhandleC", fhandleC.getCid());
        Assert.assertEquals("contentA/subB/subC/file02.txt", fhandleC.getPath().toString());
        Assert.assertEquals(0, fhandleC.getChildren().size());

        // Check that fhandleB stayed unaffected
        LOG.info(fhandleB.toString(true));
        Assert.assertEquals(fhandleBstr, fhandleB.toString(true));

        // Check that fhandleC root contains the changes
        FHandle fhandleD = fhandleC.getRoot();
        LOG.info(fhandleD.toString(true));
        Assert.assertEquals("contentA", fhandleD.getPath().toString());
        Assert.assertEquals("fhandleC", fhandleD.findChild(childPath).getCid());
    }

    @Test
    public void invalidNodesTest() throws Exception {
        
        Path rootPath = Paths.get("root");
        URL furl = new URL("http://host/path");
        FHandle fhRoot = new FHBuilder(addrBob, rootPath, furl).build();
        
        Path childPath = rootPath.resolve("childA");
        new FHBuilder(addrBob, childPath, furl).parent(fhRoot).build();
        LOG.info(fhRoot.toString(true));

        // Try another node with the same path 
        try {
            new FHBuilder(addrBob, childPath, furl).parent(fhRoot).build();
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ex) {
            // ingnore
        }

        // Try a node with some invalid path
        try {
            Path badPath = Paths.get("root");
            new FHBuilder(addrBob, badPath, furl).parent(fhRoot).build();
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ex) {
            // ingnore
        }

        // Try a node with some invalid path
        try {
            Path badPath = Paths.get("badparent/childA");
            new FHBuilder(addrBob, badPath, furl).parent(fhRoot).build();
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ex) {
            // ingnore
        }
   }
}
