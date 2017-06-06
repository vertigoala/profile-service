#!/bin/bash

# For backing up, run this script like this:
# sh backup.sh -b /data/profile-service/backup/db masterlist [1,2]

# For restore collection, run the script like this:
# sh backup.sh -r /data/profile-service/backup/db masterlist

option="$1"
currentDB="$2"
backupFolder="$3"

backupCollections() {
  backupPath="${backupFolder}/${backupName}"
  rm -rf $backupPath

  echo "Backing up Opus: ${opusUuids} into ${backupPath} \n"

  mkdir -p $backupPath

  local extractedOpusIds=$(mongo $currentDB --quiet --eval "db.opus.find({uuid: { \$in: $opusUuids}}).forEach(function(opus) { print(opus._id + ',');})")
  local opusIds="[$(echo $extractedOpusIds | sed 's/ //g' | sed 's/\(.*\),/\1/')]"
  echo "Extracted Opus Ids: ${opusIds}"

  mongodump -d $currentDB -c profile --query "{opus: {\$in : ${opusIds}}}" -o $backupPath

  #echo "${opusUuids}" > $backupPath/opusUuids.txt
  printf "$opusUuids" | sed 's/[][]//g' > $backupPath/opusUuids.txt
  #printf $opusUuids | egrep -o "A-Za-z0-9,+" > $backupPath/opusUuids.txt
  # printf $opusUuids > $backupPath/opusUuids.txt
  # printf $opusIds > $backupPath/opusIds.txt
}

restoreCollections() {

  local tempdir=$backupFolder/tempdir
  local tempStr=`echo "${backupNames}" | egrep -o "[a-z0-9A-Z\-\,]+"`
  local restoreOpusUuids="["
  local i=0
  #Loop
  for backupName in $(echo $tempStr | tr "," "\n")
  do
      echo "Restoring from: ${backupFolder}/${backupName}/opusUuids.txt \n"
      if [ $i -gt 0 ]; then restoreOpusUuids+=","; fi
      #restoreOpusIds+=`cat ${backupFolder}/${backupName}/opusUuids.txt | sed 's/\s*$//' `
      restoreOpusUuids+=`cat ${backupFolder}/${backupName}/opusUuids.txt`
      (( i++ ))
  done
  restoreOpusUuids+="]"

  echo "${restoreOpusUuids}"

  local extractedOpusIds=$(mongo $currentDB --quiet --eval "db.opus.find({uuid: { \$in: $restoreOpusUuids }}).forEach(function(opus) { print(opus._id + ',');})")
  local opusIds="[$(echo $extractedOpusIds | sed 's/ //g' | sed 's/\(.*\),/\1/')]"
  echo "Extracted Opus Ids: ${opusIds}"

  echo "creating ${tempdir}"

  rm -rf $tempdir

  mongodump -d $currentDB -o $tempdir

  mongodump -d $currentDB -c profile --query "{opus: {\$nin : ${opusIds}} }" -o $tempdir

  mongorestore --drop -d $restoreDB $tempdir/$currentDB

  for backupName in $(echo $tempStr | tr "," "\n")
  do
    mongorestore -d $restoreDB $backupFolder/$backupName/$currentDB &
    wait
  done

  rm -rf $tempdir
}

echo "You selected ${option}\n"

if [ $option == "-b" ]
then
    backupName="$4"
    opusUuids="$5"
    backupCollections
elif [ $option == "-r" ]
then
    backupNames="$4"
    restoreDB="$5"
    restoreCollections
else
    echo "Please enter first parameter b for backup and r for restore. eg: sh backup.sh -b profiles /data/profile-service/backup/db masterlist [1]"
fi

# mongodump -d profiles -c profile --query '{opus: {$in : [1]} }' -o /data/profile-service/backup/db/testme