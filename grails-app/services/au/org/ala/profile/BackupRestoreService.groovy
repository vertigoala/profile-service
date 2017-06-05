package au.org.ala.profile

import org.springframework.scheduling.annotation.Async

class BackupRestoreService {

    /**
     * Call bash script to create a backup for the one or more collections
     *
     * @param backupOpusUuids: list of uuids for eg: ["5547200f-94ee-4725-b5d1-08daeeb33ad4", "bf6bf7f5-56d4-438b-84ec-045870115200"]
     * @param outputDir: backup folder where the backup is to be created for eg: "/data/profile-service/backup/db"
     * @param backupName: backup file name
     */
    @Async
    void backupCollections(def backupOpusUuids, String outputDir, String backupName) {
     /*   List<String> ids = []
        backupOpusUuids.each {
            Opus opus = Opus.findByUuid(it)
            ids.add(opus.getId().toString())
        }*/
        //executeOnShell("sh scripts/mongo/backup.sh -b ${outputDir} ${backupName} [${ids.join(",")}]")
        executeOnShell("sh scripts/mongo/backup.sh -b ${outputDir} ${backupName} ${backupOpusUuids.toString()}")
    }

    /**
     * Call bash script to restore one or more backup files
     *
     * @param backupDir: backup folder where the backup is to be restored from for eg: "/data/profile-service/backup/db"
     * @param backupNames: list of backupDirs for eg: ["test", "masterlist"]
     * @param restoreDB: DB name to be restored to
     */
    @Async
    void restoreCollections(String backupDir, def backupNames, String restoreDB) {
        executeOnShell("sh scripts/mongo/backup.sh -r ${backupDir} ${backupNames.toString()} ${restoreDB}")
    }

    private executeOnShell(String command) {
        Process process = command.execute()
        def out = new StringBuffer()
        def err = new StringBuffer()
        process.consumeProcessOutput( out, err )
        process.waitForProcessOutput(out, err)
        log.info(out)
        log.info(err)

        return process.exitValue()
    }

}
