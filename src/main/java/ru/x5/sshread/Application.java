package ru.x5.sshread;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

public class Application {

    private static Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        try {
            File resultFile = new File("./result.txt");
            Files.write(resultFile.toPath(), new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            List<String> servers = Files.readAllLines(new File(args[0]).toPath()).stream().map(String::trim)
                    .collect(Collectors.toList());
            for (String server : servers) {
                try {
                    String resultText = server + " " + getInfoFromServer(server);
                    log.info(resultText);
                    Files.write(resultFile.toPath(), (resultText + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    log.error(server + " error " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private static String getInfoFromServer(String server) throws IOException {
        File bufferDirectory = new File("./buffer");
        bufferDirectory.mkdirs();
        File destFile = new File(bufferDirectory, server + ".properties");

        try {
            StandardFileSystemManager manager = new StandardFileSystemManager();
            manager.init();

            //Setup our SFTP configuration
            FileSystemOptions opts = new FileSystemOptions();
            SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(
                    opts, "no");
            SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, false);
            SftpFileSystemConfigBuilder.getInstance().setTimeout(opts, 10000);

            // Create local file object
            FileObject local = manager.resolveFile(destFile.getAbsolutePath());

            // Create remote file object
            FileObject remote = manager.resolveFile(
                    "sftp://" + "mgmgkappl" + ":" + "1" + "@" + server + "/" + "usr/local/gkretail/bo/config/server/parameter/integration_sappi.properties"
                    , opts);

            local.copyFrom(remote, Selectors.SELECT_SELF);

            local.close();
            remote.close();

        } finally {
        }
        return Files.readAllLines(destFile.toPath()).stream().filter(s -> s.contains("MercuryExportChannel"))
                .findAny().orElse("not found");
    }
}
