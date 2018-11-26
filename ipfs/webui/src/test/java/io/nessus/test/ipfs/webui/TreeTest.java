package io.nessus.test.ipfs.webui;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.ipfs.portal.TreeData;
import io.nessus.ipfs.portal.TreeData.TreeNode;

public class TreeTest {

    private static final Logger LOG = LoggerFactory.getLogger(TreeTest.class);
    
    @Test
    public void testApplicationPath() {
        
        TreeData tree = new TreeData();
        tree.addNode(new TreeNode("Root")).lastNode()
            .addChild(new TreeNode("Child 1"))
            .addChild(new TreeNode("Child 2"));
        
        LOG.info(tree.toString(true));
        
        List<TreeNode> data = tree.getData();
        Assert.assertEquals(1, data.size());
        Assert.assertEquals("Root", data.get(0).getText());
        
        List<TreeNode> children = data.get(0).getChildren();
        Assert.assertEquals(2, children.size());
        Assert.assertEquals("Child 1", children.get(0).getText());
        Assert.assertEquals("Child 2", children.get(1).getText());
    }
}
