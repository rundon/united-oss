<!DOCTYPE html>
<html xmlns:v-on="http://www.w3.org/1999/xhtml">
<head>
    <meta charset="UTF-8">
    <title>${fileTitle}</title>
    <link rel="stylesheet" href="plugins/layui_2.x/css/layui.css">
    <link rel="stylesheet" href="css/font-awesome.min.css"/>
    <link rel="stylesheet" href="css/document.css"/>
</head>

<body>
<div class="doc-warp" id="project_app">
    <!--搜索-->
    <div class="search-item">
        <div class="layui-form-item">

        </div>
    </div>
    <!--主体-->
    <div class="doc-main">
        <div class="down-btn">
            <button class="layui-btn layui-btn-normal" v-show="replyshow" @click="replyfun()"><i class="fa fa-reply"
                                                                                                 aria-hidden="true"></i>
            </button>
        </div>
        <div class="doc-item">
            <div class="doc-box" v-for="(item,index) in attached"
                 v-on:click="openWindows(item.directory,item.childList,item.fileName,item.fileKey,item.originName,item.parentFileName)"
                 :title="item.originName">
                <div :class="{'doc-img':true,
                'doc-large-img': (item.originName.indexOf('.jpg') != -1||item.originName.indexOf('.png') != -1),
                'doc-large-word':item.originName.indexOf('.doc') != -1,
                'doc-large-xls':item.originName.indexOf('.xls') != -1,
                'doc-large-pdf':item.originName.indexOf('.pdf') != -1,
                'doc-large-ppt':item.originName.indexOf('.ppt') != -1,
                'doc-large-zip':item.originName.indexOf('.zip') != -1,
                'doc-large-txt':item.originName.indexOf('.txt') != -1,
                'doc-large-html':(item.originName.indexOf('.html')!=-1||item.originName.indexOf('.js')!= -1||item.originName.indexOf('.css') != -1),
                'doc-large-file':item.directory}"></div>
                <div class="doc-name">
                    <a>{{item.originName}}</a>
                </div>
            </div>

        </div>
    </div>
</div>
<script type="text/javascript" src="js/jquery.min.js"></script>
<script type="text/javascript" src="js/common.js"></script>
<script type="text/javascript" src="libs/vue.min.js"></script>
<script type="text/javascript" src="plugins/layui_2.x/layui.js"></script>
<script type="text/javascript">
    var json = "";
    $(function () {
        json = JSON.parse('${fileTree}');
        vm.attached = json.childList;
        var url = getQueryVariable("url");
        var prekey = getQueryVariable("prekey");
        if (url) {
            if (url.indexOf('.zip') == -1||url.indexOf("http")>=0) {
            } else {
                vm.replyshow = true;
            }
        }
        if (prekey) {
            var node = loadParent(decodeURIComponent(prekey), json);
            vm.attached = node.childList;
            vm.replyshow = true;
            var mm = loadParentId(node, node.parentFileName);
            vm.parentDirName = mm;
        }
    });

    function loadParentId(node, name) {
        var parentnode = loadParent(node.parentFileName, json)
        if (parentnode.parentFileName == "") {
            return name;
        }
        name = name + "," + parentnode.parentFileName;
        return loadParentId(parentnode, name);
    }

    function getQueryVariable(variable) {
        var query = window.location.search.substring(1);
        var vars = query.split("&");
        for (var i = 0; i < vars.length; i++) {
            var pair = vars[i].split("=");
            if (pair[0] == variable) {
                return pair[1];
            }
        }
        return (false);
    }

    var vm = new Vue({
        el: "#project_app",
        data: {
            attached: [],//当前
            replyshow: false,
            parentDirName: ""
        },
        methods: {
            loadTarget: function (item) {
                vm.attached = item;
            },
            openWindows: function (isdir, childList, fileName, fileKey, originName, parentFileName) {
                if (isdir) {
                    vm.attached = childList;
                    vm.replyshow = true;
                    if (vm.parentDirName == "") {
                        vm.parentDirName = parentFileName;
                    } else {
                        vm.parentDirName = vm.parentDirName + "," + parentFileName;
                    }

                } else {
                    if (fileName.substring(fileName.length - 4).indexOf(".zip") == -1) {
                        window.open("onlinePreview?url=" + encodeURIComponent(fileName) + "&name=" + originName + "&access_token=" + getQueryVariable("access_token") + "&id=" + getQueryVariable("id"));
                    } else {
                        var newurl = updateQueryStringParameter(window.location.href, 'prekey', parentFileName);
                        window.history.replaceState({
                            path: newurl
                        }, '', newurl);
                        location.href = "onlinePreview?url=" + encodeURIComponent(fileName) + "&fileKey=" + encodeURIComponent(fileKey) + "&access_token=" + getQueryVariable("access_token") + "&id=" + getQueryVariable("id");
                    }
                }
            },
            replyfun: function () {
                var url = getQueryVariable("url");
                if (url != ""&&url.indexOf("http")<0) {
                    window.location.replace(document.referrer)
                    //window.history.back();
                } else {
                    var ids = vm.parentDirName.split(",").sort();
                    var id = ids[ids.length - 1];
                    var chil = loadParent(id, json);
                    if (ids.length > 1) {
                        for (var i = 0; i < ids.length - 1; i++) {
                            if (i == 0) {
                                vm.parentDirName = ids[i];
                            } else {
                                vm.parentDirName = vm.parentDirName + "," + ids[i];
                            }
                        }
                    }
                    if (chil.parentFileName == "") {
                        vm.attached = chil.childList;
                        vm.replyshow = false;
                    } else {
                        vm.attached = chil.childList;
                        vm.replyshow = true;
                    }
                }
            }
        }
    })

    function loadParent(parentFileName, data) {
        if (data.fileName == parentFileName) {
            return data;
        } else {
            for (var node of data.childList) {
                const text = loadParent(parentFileName, node);
                if (text) {
                    return text;
                }
            }
            ;
        }
    }

    function updateQueryStringParameter(uri, key, value) {
        if (!value) {
            return uri;
        }
        var re = new RegExp("([?&])" + key + "=.*?(&|$)", "i");
        var separator = uri.indexOf('?') !== -1 ? "&" : "?";
        if (uri.match(re)) {
            return uri.replace(re, '$1' + key + "=" + value + '$2');
        } else {
            return uri + separator + key + "=" + value;
        }
    }
</script>
</body>
</html>