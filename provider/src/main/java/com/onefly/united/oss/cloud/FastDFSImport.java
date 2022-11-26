package com.onefly.united.oss.cloud;

import com.github.tobato.fastdfs.FdfsClientConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 导入FastDFS-Client组件
 */
@Configuration
@Import(FdfsClientConfig.class)
public class FastDFSImport {

}