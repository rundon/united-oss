$(function () {
    vm.loadTarget(-1);
});

//鼠标移入
function showHover(obj) {
    var myradio = $(obj).find(".radio-slt");
    var input_check = $(obj).find("input[name='check']");
    var rename_box = $(obj).find(".rename-box")
    var file_name = $(obj).find(".doc-name a");
    myradio.addClass("show");
    $(obj).addClass("hover");
}
//鼠标移除
function hideHover(obj) {
    var myradio = $(obj).find(".radio-slt");
    var input_check = $(obj).find("input[name='check']");
    if(!input_check.is(':checked')) {
        myradio.removeClass("show");
        $(obj).removeClass("hover");
    }
}

Array.prototype.push2 =function(arg){
    if (this.indexOf(arg) == -1) {
        this.push(arg);
    }
};

Array.prototype.indexOf = function(val) {
    for (var i = 0; i < this.length; i++) {
        if (this[i] == val) return i;
    }
    return -1;
};

Array.prototype.remove = function(val) {
    var index = this.indexOf(val);
    if (index > -1) {
        this.splice(index, 1);
    }
};

var vm = new Vue({
    el: "#project_app",
    data:{
        target:[],
        attached:[],
        previousTargetId:null,
        checkBoxValue:[],
        originalName:null
    },
    methods:{
        loadTarget:function (id) {
            // if (isBlank(id)) {
            //     id = -1;
            // }
            // $.ajax({
            //     // url:baseURL + '/pms/pmtarget/getTargetList/' + id,
            //     url:'http://pms.31education.com//pms/pmtarget/getTargetList/577',
            //     type:'POST',
            //     success:function (r) {
            //         if (r.code == 0) {
            //             vm.target = r.target;
            //             vm.target.forEach(function (value,index,array) {
            //                 value.status = 0
            //             });
            //             vm.loadAttached(id);
            //             vm.checkBoxValue = [];
            //             vm.checkBoxFileValue = [];
            //             if (r.parentTarget != null) {
            //                 vm.previousTargetId = r.parentTarget.ptarget;
            //             }
            //         }
            //     }
            // });
        },
        loadAttached:function (id) {
            // $.ajax({
            //     // url:baseURL + '/pms/attached/getAttachedByTid/' + id,
            //     url:'http://pms.31education.com//pms/attached/getAttachedByTid/577',
            //     type:'POST',
            //     success:function (r) {
            //         if (r.code == 0) {
            //             if (vm.target.length == 0 && (r.attached == null||r.attached.length == 0)) {
            //                 alert("此文件为空")
            //                 vm.attached=r.attached;
            //             } else {
            //                 vm.attached = r.attached;
            //             }
            //         }
            //     }
            // });
        },
        checkFileItem:function(item){
            if (item.status) {
                vm.checkBoxFileValue.push2(item.id);
            } else {
                vm.checkBoxFileValue.remove(item.id);
            }
        },
        checkItem:function (val,v1,v2) {
            window.open("onlinePreview?url="
                + encodeURIComponent(val + v1)+"&fileKey="+ encodeURIComponent(v2));
        },
        downloadAllFile:function () {
            if (vm.checkBoxFileValue.length == 0 && vm.checkBoxValue.length == 0 ) {
                alert("请选择要下载的文件");
                return false
            }

            if (vm.checkBoxValue.length > 0) {
                vm.checkBoxValue.forEach(function (value,index,array) {
                    window.open(baseURL + '/pms/download/' + value)
                });
            }

            if (vm.checkBoxFileValue.length > 0) {
                vm.checkBoxFileValue.forEach(function (value,index,array) {
                    window.open(baseURL + '/pms/download/zip/' + value)
                });
            }

        },
        searchFile:function () {
            if (isBlank(vm.originalName)) {
                alert("请输入文件名");
                return false;
            }
            $.ajax({
                url:baseURL + '/pms/attached/search/' + vm.originalName,
                type:'POST',
                success:function (r) {
                    vm.attached = r.attached;
                    vm.target = [];
                }
            })
        }
    }
});
