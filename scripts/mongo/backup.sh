#!/bin/bash

# For backing up, run this script like this:
# sh backup.sh -b /data/profile-service/backup/db masterlist [1,2]

# For restore collection, run the script like this:
# sh backup.sh -r /data/profile-service/backup/db masterlist

option="$1"
backupFolder="$2"

backupCollections() {
  backupPath="${backupFolder}/${backupName}"
  rm -rf $backupPath

  echo "Backing up Opus: ${opusUuids} into ${backupPath} \n"

  mkdir -p $backupPath

  local extractedOpusIds=$(mongo profiles --quiet --eval "db.opus.find({uuid: { \$in: $opusUuids}}).forEach(function(opus) { print(opus._id + ',');})")
  local opusIds="[$(echo $extractedOpusIds | sed 's/ //g' | sed 's/\(.*\),/\1/')]"
  echo "Extracted Opus Ids: ${opusIds}"

  mongodump -d profiles -c profile --query "{opus: {\$in : ${opusIds}}}" -o $backupPath

  #echo "${opusUuids}" > $backupPath/opusUuids.txt
  printf "$opusUuids" | sed 's/[][]//g' > $backupPath/opusUuids.txt
  #printf $opusUuids | egrep -o "A-Za-z0-9,+" > $backupPath/opusUuids.txt
  # printf $opusUuids > $backupPath/opusUuids.txt
  # printf $opusIds > $backupPath/opusIds.txt
}

restoreCollections() {

  mongo $restoreDB --eval "db.dropDatabase()"

  local tempdir=$backupFolder/tempdir
  local tempStr=`echo "${backupNames}" | egrep -o "[a-z0-9A-Z\-\,]+"`
  local restoreOpusUuids=""
  local restoreOpusIds="["
  local i=0
  #Loop
  for backupName in $(echo $tempStr | tr "," "\n")
  do
      echo "Restoring from: ${backupFolder}/${backupName}/opusUuids.txt \n"
      if [ $i -gt 0 ]; then restoreOpusIds+=","; fi
      restoreOpusIds+=`cat ${backupFolder}/${backupName}/opusUuids.txt | sed 's/\s*$//' `
      (( i++ ))
  done
  restoreOpusIds+="]"

  # read opusIds.txt for the opusIds
 # local restoreOpusIds=`cat ${opusIdFiles}`

  echo "$restoreOpusIds"

  local extractedOpusIds=$(mongo profiles --quiet --eval "db.opus.find({uuid: { \$in: $restoreOpusIds}}).forEach(function(opus) { print(opus._id + ',');})")
  local opusIds="[$(echo $extractedOpusIds | sed 's/ //g' | sed 's/\(.*\),/\1/')]"
  echo "Extracted Opus Ids: ${opusIds}"

  echo "creating ${tempdir}"

  rm -rf $tempdir

  mongodump -d profiles -o $tempdir

  mongodump -d profiles -c profile --query "{opus: {\$nin : ${opusIds}} }" -o $tempdir
  mongorestore -d $restoreDB $tempdir/profiles

  for backupName in $(echo $tempStr | tr "," "\n")
  do
    mongorestore -d $restoreDB $backupFolder/$backupName/profiles &
    wait
  done

  rm -rf $tempdir
 # mongorestore -d profiles-restore $backupPath/profiles
}

echo "You selected ${option}\n"

if [ $option == "-b" ]
then
    backupName="$3"
    opusUuids="$4"

    backupCollections
elif [ $option == "-r" ]
then
    backupNames="$3"
    restoreDB="$4"
    restoreCollections
else
    echo "Please enter first parameter b for backup and r for restore. eg: sh backup.sh -b /data/profile-service/backup/db masterlist [1]"
fi

# mongodump -d profiles -c profile --query '{opus: {$in : [1]} }' -o /data/profile-service/backup/db/testme