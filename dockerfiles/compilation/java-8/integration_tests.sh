#!/bin/sh -e

printUsage() {
   echo "Usage : "
   echo "./integration_tests.sh URL BRANCH"
   echo "    SHA1(optional): Branch to build or master if none"
   exit 1
}

ORIGIN=/origin

for arg in "$@"
do
   case $arg in
      -*)
         echo "Invalid option: -$OPTARG"
         printUsage
         ;;
      *)
         if ! [ -z "$1" ]; then
            SHA1=$1
         fi
         ;;
   esac
done

if [ -z "$SHA1" ]; then
   SHA1=master
fi

echo $SHA1

git clone $ORIGIN/.
git checkout $SHA1

mvn clean package -Pvalidating-deployment -pl org.apache.james:deployment-testing
