package io.nessus.ipfs.portal;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.nessus.ipfs.jaxrs.SFHandle;

public class TreeData {
    
    final List<TreeNode> data = new ArrayList<>();

    public List<TreeNode> getData() {
        return Collections.unmodifiableList(data);
    }
    
    public TreeData addNode(TreeNode node) {
        data.add(node);
        return this;
    }
    
    public TreeNode lastNode() {
        int size = data.size();
        if (size == 0) return null;
        return data.get(size - 1);
    }
    
    public String toString(boolean pretty) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectWriter writer = mapper.writer();
            if (pretty) writer = mapper.writerWithDefaultPrettyPrinter();
            return writer.writeValueAsString(this);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    public String toString() {
        return toString(false);
    }

    public static class TreeNode {
        
        final String text;
        final Map<String, Object> data = new LinkedHashMap<>();
        final List<TreeNode> children = new ArrayList<>();

        public TreeNode(String text) {
            this.text = text;
        }

        public TreeNode(TreeNode parent, SFHandle sfh) {
            
            Path path = Paths.get(sfh.getPath());
            String cid = sfh.getCid();
            
            data.put("addr", sfh.getOwner());
            data.put("path", sfh.getPath());
            
            if (parent == null && cid != null) {
                text = String.format("%s %s", cid, path);
            } else {
                text = String.format("%s", path.getFileName());
            }
            
            // [A] Top level file 
            // [B] Top level directory
            // [D] Sub level file
            if (parent == null || sfh.getChildren().isEmpty()) {
                data.put("cid", cid);
            }
        }

        public String getText() {
            return text;
        }

        public TreeNode addChild(TreeNode node) {
            children.add(node);
            return this;
        }
        
        public TreeNode lastChild() {
            int size = children.size();
            if (size == 0) return null;
            return children.get(size - 1);
        }
        
        @JsonInclude(Include.NON_EMPTY)
        public List<TreeNode> getChildren() {
            return Collections.unmodifiableList(children);
        }

        @JsonInclude(Include.NON_EMPTY)
        public Map<String, Object> getData() {
            return data;
        }
    }
}
