<!DOCTYPE html>

<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, user-scalable=yes, initial-scale=1.0">
    <title>PDF预览</title>
    <style type="text/css">
        * {
            margin: 0;
            padding: 0;
        }

        html, body {
            height: 100%;
            width: 100%;
        }
    </style>
</head>
<body>
<iframe src="" width="100%" frameborder="0"></iframe>
<#if disableSwitch==false>
    <img src="images/jpg.svg" width="63" height="63"
         style="position: fixed; cursor: pointer; top: 40%; right: 48px; z-index: 999;" alt="使用图片预览" title="使用图片预览"
         onclick="goForImage()"/>
</#if>
</body>
<script src="js/watermark.js" type="text/javascript"></script>
<script type="text/javascript">

    /**
     * 页面变化调整高度
     */
    window.onresize = function () {
        var fm = document.getElementsByTagName("iframe")[0];
        fm.height = window.document.documentElement.clientHeight - 10;
    }

    function goForImage() {
        var url = window.location.href;
        if (url.indexOf("officePreviewType=pdf") != -1) {
            url = url.replace("officePreviewType=pdf", "officePreviewType=image");
        } else {
            url = url + "&officePreviewType=image";
        }
        window.location.href = url;
    }

    /*初始化水印*/
    window.onload = function () {
        debugger
        var str = document.URL;
        var pdfUrl="${pdfUrl}";
        var finalUrl;
        if(pdfUrl.indexOf("http://")!=-1||pdfUrl.indexOf("https://")!=-1){
            finalUrl=pdfUrl;
        }else{
            finalUrl="/preview/"+pdfUrl;
        }
        console.log(finalUrl);
        document.getElementsByTagName('iframe')[0].src = "pdfjs/web/viewer.html?file=" + encodeURIComponent(finalUrl) + "&disabledownload=${pdfDownloadDisable}"+"&timestamp="+new Date().getTime();
        document.getElementsByTagName('iframe')[0].height = document.documentElement.clientHeight - 10;
        var watermarkTxt = '${watermarkTxt}';
        if (watermarkTxt !== '') {
            watermark.init({
                watermark_txt: '${watermarkTxt}',
                watermark_x: 0,
                watermark_y: 0,
                watermark_rows: 0,
                watermark_cols: 0,
                watermark_x_space: ${watermarkXSpace},
                watermark_y_space: ${watermarkYSpace},
                watermark_font: '${watermarkFont}',
                watermark_fontsize: '${watermarkFontsize}',
                watermark_color: '${watermarkColor}',
                watermark_alpha: ${watermarkAlpha},
                watermark_width: ${watermarkWidth},
                watermark_height: ${watermarkHeight},
                watermark_angle: ${watermarkAngle},
            });
        }
    }
</script>
</html>