
// https://github.com/vakata/jstree

function treeMenuIpfs(node) {

    var jstree = $('#treeIpfs').jstree(true);
    var data = jstree.get_selected(true)[0].data;
    console.log("treeIpfs: " + data);
        
    /** 
     * There are four types of IPFS nodes
     * 
     * [A] Top level file 
     * [B] Top level directory
     * [C] Sub level directory
     * [D] Sub level file
     * 
     * Actions apply as follows
     * 
     * [A] get, show, send, remove
     * [B] get, ----, send, remove
     * [C] ---, ----, ----, ------
     * [D] ---, show, ----, ------
     */

    var hasParent = node.parent != "#";
    var hasChildren = node.children.length > 0;
    
    var typeA = !hasParent && !hasChildren;
    var typeB = !hasParent && hasChildren;
    var typeC = hasParent && hasChildren;
    var typeD = hasParent && !hasChildren;
    
    var hrefGet = "/portal/pget?addr=" + data.addr + "&path=" + data.path + "&cid=" + data.cid;
    var hrefShow = data.gatewayUrl + "/" + data.cid;
    var hrefSend = "/portal/psend?addr=" + data.addr + "&path=" + data.path + "&cid=" + data.cid;
    var hrefRemove = "/portal/rmipfs?addr=" + data.addr + "&cids=" + data.cid;

    var itemGet = { 
    		"label": "get", 
            "_disabled": typeC || typeD,
    		"action": function (obj) { window.open(hrefGet, "_self"); } 
    }
    var itemShow = { 
    		"label": "show", 
            "_disabled": typeB || typeC,
    		"action": function (obj) { window.open(hrefShow); } 
    }
    var itemSend = { 
    		"label": "send", 
            "_disabled": typeC || typeD || data.nosend,
    		"action": function (obj) { window.open(hrefSend, "_self"); } 
    }
    var itemRemove = { 
    		"label": "remove", 
            "_disabled": typeC || typeD,
    		"action": function (obj) { window.open(hrefRemove, "_self"); } 
    }
    
    var items = {
	        "get": itemGet,
	        "show": itemShow,
	        "send": itemSend,
	        "remove": itemRemove
    }

    return items;
}

function treeMenuLocal(node) {

    var jstree = $('#treeLocal').jstree(true);
    var data = jstree.get_selected(true)[0].data;
    console.log("treeLocal: " + data);
    
    /** 
     * There are four types of IPFS nodes
     * 
     * [A] Top level file 
     * [B] Top level directory
     * [C] Sub level directory
     * [D] Sub level file
     * 
     * Actions apply as follows
     * 
     * [A] add, show, remove
     * [B] add, ----, remove
     * [C] add, ----, remove
     * [D] add, show, remove
     */

    var hasParent = node.parent != "#";
    var hasChildren = node.children.length > 0;
    
    var typeA = !hasParent && !hasChildren;
    var typeB = !hasParent && hasChildren;
    var typeC = hasParent && hasChildren;
    var typeD = hasParent && !hasChildren;
    
    var hrefAdd = "/portal/addpath?addr=" + data.addr + "&path=" + data.path;
    var hrefShow = "/portal/fshow?addr=" + data.addr + "&path=" + data.path;
    var hrefRemove = "/portal/rmlocal?addr=" + data.addr + "&path=" + data.path;

    var itemAdd = { 
    		"label": "add", 
    		"action": function (obj) { window.open(hrefAdd, "_self"); } 
    }
    var itemShow = { 
    		"label": "show", 
            "_disabled": typeB || typeC,
    		"action": function (obj) { window.open(hrefShow); } 
    }
    var itemRemove = { 
    		"label": "remove", 
    		"action": function (obj) { window.open(hrefRemove, "_self"); } 
    }
    
    var items = {
	        "add": itemAdd,
	        "show": itemShow,
	        "remove": itemRemove
    }

    return items;
}
