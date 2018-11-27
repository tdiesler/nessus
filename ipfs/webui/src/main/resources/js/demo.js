
// https://github.com/vakata/jstree

function treeMenuIpfs(node) {

    // Directory items have no context menu
    if (node.children.length > 0) {
        return "";
    }
    
    var jstree = $('#treeIpfs').jstree(true);
    var data = jstree.get_selected(true)[0].data;
    console.log("treeIpfs: " + data);
        
    var items = {
        "show": {
            "label": "show",
            "action": function (obj) { 
                var href = "$gatewayUrl/" + data.cid;
                window.open(href); 
                }
        },
        "get": {
            "label": "get",
            "action": function (obj) { 
                var href = "/portal/pget?addr=" + data.addr + "&path=" + data.path + "&cid=" + data.cid;
                window.open(href, "_self"); 
                }
        },
        "send": {
            "label": "send",
            "action": function (obj) { 
                var href = "/portal/psend?addr=" + data.addr + "&path=" + data.path + "&cid=" + data.cid;
                window.open(href, "_self"); 
                }
        },
        "remove": {
            "label": "remove",
            "action": function (obj) { 
                var href = "/portal/rmipfs?addr=" + data.addr + "&cids=" + data.cid;
                window.open(href, "_self"); 
            }
        }
    }

    return items;
}

function treeMenuLocal(node) {

    var jstree = $('#treeLocal').jstree(true);
    var data = jstree.get_selected(true)[0].data;
    console.log("treeLocal: " + data);
    
    var items = {
        "show": {
            "label": "show",
            "action": function (obj) { 
                var href = "/portal/fshow?addr=" + data.addr + "&path=" + data.path;
                window.open(href); 
                }
        },
        "remove": {
            "label": "remove",
            "action": function (obj) { 
                var href = "/portal/rmlocal?addr=" + data.addr + "&path=" + data.path;
                window.open(href, "_self"); 
            }
        }
    }

    // Directory items have no show
    if (node.children.length > 0) {
    	delete items.show;
    }
    
    return items;
}
