#!/bin/bash
# hoojo @hooj0.github.com
# 2018-08-22

#set -e
#set -uo pipefail
trap "echo 'error: Script failed: see failed command above'" ERR


# import files
# -------------------------------------------------------------------------------
#. ../scripts/log.sh
function log() {
	# 字颜色：30—–37
	# 字背景颜色范围：40—–47
	case "$1" in
		"red")
			echo -e "\033[31m$2\033[0m" # 红色字
		;; 
		"yellow")
			echo -e "\033[33m$2\033[0m" # 黄色字
		;; 
		"green")
			echo -e "\033[32m$2\033[0m" # 绿色字
		;; 
		"blue")
			echo -e "\033[34m$2\033[0m" # 蓝色字
		;; 
		"purple")
			echo -e "\033[35m$2\033[0m" # 紫色字
		;; 
		"sky_blue")
			echo -e "\033[36m$2\033[0m" # 天蓝字
		;; 
		"white")
			echo -e "\033[37m$2\033[0m" # 白色字
		;; 
		"_black")
			echo -e "\033[40;37m $2 \033[0m" # 黑底白字
		;; 
		"_red")
			echo -e "\033[41;30m $2 \033[0m" # 红底黑字
		;; 
		*)
			echo "$2"
		;;
	esac
}

# common variables
# -------------------------------------------------------------------------------
FABRIC_ROOT="/opt/gopath/src/github.com/hyperledger/fabric"
export FABRIC_ROOT=$FABRIC_ROOT
export FABRIC_CFG_PATH=$PWD

log purple "FABRIC_ROOT=${FABRIC_ROOT}"
log purple "FABRIC_CFG_PATH=${FABRIC_CFG_PATH}"
echo

OS_ARCH=$(echo "$(uname -s|tr '[:upper:]' '[:lower:]'|sed 's/mingw64_nt.*/windows/')-$(uname -m | sed 's/x86_64/amd64/g')" | awk '{print tolower($0)}')

function usageHelp() {
cat << HELP
USAGE: $0 [OPTIONS] COMMANDS

OPTIONS: 
  -h help 		use the help manual.
  -v version 		fabric configtx generate config version(default: v1.1).
  -c channel		generate channel configtx files(default: mycc)
  -a anchor peer	generate anchor peer config tx files

COMMANDS:
  clean 		clean store & config
  gen 			generate channel & artifacts & certificates
  gen-channel 		generate "increment" channel configtx artifacts
  merge 		merge channel & artifacts & certificates to version directory
  regen 		regenerate channel & artifacts & certificates
	
EXAMPLE: 
  $0 -h
  $0 help

  $0 -v v1.1 -c mycc gen
  $0 -v v1.1 -c mycc gen merge
  $0 -v v1.1 -c mycc regen
  $0 -v v1.1 -c mycc regen merge
  $0 -c mychannel -c mycc gen-channel
  $0 -c mychannel -v v1.2 gen-channel
	
  $0 -a -c mychannel -c mycc clean gen-channel
  $0 -c mychannel -v v1.1 merge
  $0 -c mychannel -v v1.1 clean
	
HELP
exit 0
}

## Using docker-compose template replace private key file names with constants
function replacePrivateKey () {
	echo
	echo "##########################################################"
	echo "#####         replace certificates  key          #########"
	echo "##########################################################"
	
	ARCH=`uname -s | grep Darwin`
	echo "ARCH: $ARCH"
	if [ "$ARCH" == "Darwin" ]; then
		OPTS="-it"
	else
		OPTS="-i"
	fi
	echo "OPTS: $OPTS"

    echo
    log yellow "==> cp -r scripts/compose-script-template.sh scripts/$VERSION_DIR/compose-script.sh"
    mkdir -pv scripts/$VERSION_DIR/
	cp -rv scripts/compose-script-template.sh scripts/$VERSION_DIR/compose-script.sh

    CURRENT_DIR=$PWD
    cd ./$CRYPTO_CONFIG_LOCATION/peerOrganizations/org1.foo.com/ca/
    PRIV_KEY=$(ls *_sk)
    cd $CURRENT_DIR
	
    sed $OPTS "s/CA1_PRIVATE_KEY/${PRIV_KEY}/g" scripts/$VERSION_DIR/compose-script.sh 

    cd ./$CRYPTO_CONFIG_LOCATION/peerOrganizations/org2.bar.com/ca/
    PRIV_KEY=$(ls *_sk)
    cd $CURRENT_DIR

    sed $OPTS "s/CA2_PRIVATE_KEY/${PRIV_KEY}/g" scripts/$VERSION_DIR/compose-script.sh 
    
	log green "replace sk......Done!"
	echo
}

## Generates Org certs using cryptogen tool
function generateCerts() {
	CRYPTOGEN=$FABRIC_ROOT/release/$OS_ARCH/bin/cryptogen

	if [ -f "$CRYPTOGEN" ]; then
        log yellow "Using cryptogen -> $CRYPTOGEN"
		log green "check crypto......Done!"
	else
	    log yellow "Building cryptogen"
	    log yellow "===> make -C $FABRIC_ROOT release"
	    make -C $FABRIC_ROOT release
		log green "make crypto......Done!"
	fi

	echo
	echo "##########################################################"
	echo "##### Generate certificates using cryptogen tool #########"
	echo "##########################################################"

	log yellow "==> cryptogen generate --config=./$CRYPTO_CONFIG_FILE --output=./$CRYPTO_CONFIG_LOCATION"
	$CRYPTOGEN generate --config=./$CRYPTO_CONFIG_FILE --output=./$CRYPTO_CONFIG_LOCATION
	
	log green "generate crypto......Done!"
	echo
}

## Generate orderer genesis block , channel configuration transaction and anchor peer update transactions
function checkConfigtxgen() {
	CONFIGTXGEN=$FABRIC_ROOT/release/$OS_ARCH/bin/configtxgen
	
	if [ -f "$CONFIGTXGEN" ]; then
        log yellow "Using configtxgen -> $CONFIGTXGEN"
		
		log green "check Configtxgen......Done!"
	else
	    log yellow "Building configtxgen"
	    log yellow "===> make -C $FABRIC_ROOT release"
	    make -C $FABRIC_ROOT release
		
		log green "make Configtxgen......Done!"
	fi
}

function generateGenesisBlock() {
	echo
	echo "##########################################################"
	echo "#########  Generating Orderer Genesis block ##############"
	echo "##########################################################"
	# Note: For some unknown reason (at least for now) the block file can't be
	# named orderer.genesis.block or the orderer will fail to launch!
	log yellow "==> cryptogen -profile TwoOrgsOrdererGenesis${version} -outputBlock ./$CHANNEL_ARTIFACTS_LOCATION/genesis.block"
	$CONFIGTXGEN -profile TwoOrgsOrdererGenesis${version} -outputBlock ./$CHANNEL_ARTIFACTS_LOCATION/genesis.block
	
	log green "generate genesis.block......Done!"
	echo
}

function generateChannelArtifacts() {
	echo
	echo "#################################################################"
	echo "### Generating channel configuration transaction 'channel.tx' ###"
	echo "#################################################################"
	
	for channel in "$@"; do
		log yellow "==> cryptogen -profile TwoOrgsChannel${version} -outputCreateChannelTx ./$CHANNEL_ARTIFACTS_LOCATION/$channel.tx -channelID $channel"
		$CONFIGTXGEN -profile TwoOrgsChannel${version} -outputCreateChannelTx ./$CHANNEL_ARTIFACTS_LOCATION/$channel.tx -channelID $channel
		log green "generate channel [$channel]......Done!"
		echo
		
		if [ $generateAnchorPeer == "true" ]; then
			generateAnchorPeerArtifacts $channel
		fi
	done	
}

function generateAnchorPeerArtifacts() {
	echo
	echo "#################################################################"
	echo "#######    Generating anchor peer update for Org1MSP   ##########"
	echo "#################################################################"
	log yellow "==> cryptogen -profile TwoOrgsChannel -outputAnchorPeersUpdate ./$CHANNEL_ARTIFACTS_LOCATION/Org1MSPanchors.tx -channelID $1 -asOrg Org1MSP"
	$CONFIGTXGEN -profile TwoOrgsChannel -outputAnchorPeersUpdate ./$CHANNEL_ARTIFACTS_LOCATION/Org1MSPanchors.tx -channelID $1 -asOrg Org1MSP
	
	log green "generate anchor peer[Org1MSP]......Done!"

	echo
	echo "#################################################################"
	echo "#######    Generating anchor peer update for Org2MSP   ##########"
	echo "#################################################################"
	log yellow "==> cryptogen -profile TwoOrgsChannel -outputAnchorPeersUpdate ./$CHANNEL_ARTIFACTS_LOCATION/Org2MSPanchors.tx -channelID $1 -asOrg Org2MSP"
	$CONFIGTXGEN -profile TwoOrgsChannel -outputAnchorPeersUpdate ./$CHANNEL_ARTIFACTS_LOCATION/Org2MSPanchors.tx -channelID $1 -asOrg Org2MSP
	
	log green "generate anchor peer[Org2MSP]......Done!"
	echo
}

function cleanChannelArtifacts() {

    echo
	echo "#################################################################"
	echo "#######            clean channel artifacts             ##########"
	echo "#################################################################"

	
	log yellow "==> rm -rf ./$VERSION_DIR/"
    [ -n $VERSION_DIR ] && [ -d "./$VERSION_DIR" ] && rm -rf ./$VERSION_DIR

	log yellow "==> rm -rf ./channel-artifacts ./crypto-config ./scripts/$VERSION_DIR"
    rm -rf ./channel-artifacts ./crypto-config ./scripts/$VERSION_DIR
    
	log green "clean all......Done!"
    echo
}

function createChannelArtifactsDir() {

    echo
	echo "#################################################################"
	echo "#######       create channel artifacts directory       ##########"
	echo "#################################################################"

    log yellow "==> mkdir ./channel-artifacts"
	[ ! -d "./channel-artifacts" ] && mkdir -pv ./channel-artifacts 

    log yellow "==> mkdir ./crypto-config"
	[ ! -d "./crypto-config" ] && mkdir -pv ./crypto-config 
    
	log green "create directory......Done!"
    echo
}

function mergeArtifactsCryptoDir() {
	echo
	echo "#################################################################"
	echo "#######            merge channel artifacts  files      ##########"
	echo "#################################################################"

	log yellow "==> mkdir ./channel-artifacts"
	[ ! -d "./$VERSION_DIR" ] && mkdir -pv ./$VERSION_DIR
	
	echo "==> mv ./channel-artifacts ./$VERSION_DIR/"
    mv -v ./channel-artifacts ./$VERSION_DIR/

	echo "==> mv ./crypto-config ./$VERSION_DIR/"
    mv -v ./crypto-config ./$VERSION_DIR/

	log green "merge files......Done!"
    echo
}

function copyArtifactsCryptoDir() {
	echo
	echo "#################################################################"
	echo "#######     copy channel artifacts & crypto  files     ##########"
	echo "#################################################################"

	log yellow "==> mkdir ./channel-artifacts"
	[ ! -d "./$VERSION_DIR" ] && mkdir -pv ./$VERSION_DIR
	
	echo "==> mv ./channel-artifacts ./$VERSION_DIR/"
    cp -aurv ./channel-artifacts ./$VERSION_DIR/

	echo "==> mv ./crypto-config ./$VERSION_DIR/"
    cp -aurv ./crypto-config ./$VERSION_DIR/

	log green "copy files......Done!"
    echo
}

function fetchRequiredChannelArtifacts() {
    echo
	echo "#################################################################"
	echo "#######       fetch channel artifacts directory       ##########"
	echo "#################################################################"

	requiredFiles="crypto-config/peerOrganizations/org1.foo.com/msp/cacerts"
	requiredFiles="crypto-config"
	
	if [ ! -d "$requiredFiles" ]; then
		log yellow "==> mkdir $requiredFiles"
		
		mkdir -pv $requiredFiles
		cp -aur "./$VERSION_DIR/$requiredFiles" .
	else
		log yellow "==> exist required file: $requiredFiles"
	fi
    
    log green "fetch files......Done!"
	echo
}

function moveIncrementChannelArtifacts() {
	echo
	echo "#################################################################"
	echo "#######    move increment channel artifacts file       ##########"
	echo "#################################################################"

	log yellow "==> mv ./channel-artifacts/* ./$VERSION_DIR/channel-artifacts"
    mv -iv ./channel-artifacts/* ./$VERSION_DIR/channel-artifacts
	log green "move files......Done!"
	echo
	
	log yellow "==> rm -rf ./channel-artifacts ./crypto-config"
    rm -rf ./channel-artifacts ./crypto-config
	log green "clean files......Done!"
    
    echo
}

# usage options
# -------------------------------------------------------------------------------
printf "\n\n"
#echo "参数列表：$*"

while getopts ":c:v:hau" opt; do

	printf "选项：%s, 参数值：$OPTARG \n" $opt
    case $opt in
    	c ) 
			CHANNEL_NAME="$CHANNEL_NAME $OPTARG"
		;;
		v ) 			
			VERSION_DIR="$OPTARG"
			version=`echo $VERSION_DIR | sed 's/\.//g'`
			if [[ $VERSION_DIR =~ "v" ]]; then
				version=_$version
			else
				log red "not contains version char 'v'"
				version=_v$version
			fi
		;;
		a|u ) 
			generateAnchorPeer="true"
		;;
		h ) 
			usageHelp
		;;        
        ? ) echo "error" exit 1;;
    esac
done

shift $(($OPTIND - 1))
#echo "命令参数：$*"


# variable
# ------------------------------------------------------------------------------
CHANNEL_NAME="$(echo $CHANNEL_NAME)"

: ${CHANNEL_NAME:="mycc"}
: ${CHANNEL_ARTIFACTS_LOCATION:="channel-artifacts"}
: ${CRYPTO_CONFIG_LOCATION:="crypto-config"}
: ${CRYPTO_CONFIG_FILE:="crypto-config.yaml"}
: ${VERSION_DIR:="v1.1"}
: ${version:="_v11"}
: ${generateAnchorPeer:="false"}

log purple "CHANNEL_NAME: $CHANNEL_NAME"
log purple "CHANNEL_ARTIFACTS_LOCATION: $CHANNEL_ARTIFACTS_LOCATION"
log purple "CRYPTO_CONFIG_LOCATION: $CRYPTO_CONFIG_LOCATION"
log purple "CRYPTO_CONFIG_FILE: $CRYPTO_CONFIG_FILE"
log purple "VERSION: $version"
log purple "VERSION_DIR: $VERSION_DIR"


# process
# ------------------------------------------------------------------------------
for opt in "$@"; do
	case "$opt" in
        clean)
            cleanChannelArtifacts
        ;;
        gen)
			checkConfigtxgen
            createChannelArtifactsDir
            generateCerts
			replacePrivateKey
			generateGenesisBlock

			generateChannelArtifacts $CHANNEL_NAME
			#generateAnchorPeerArtifacts
        ;;
        gen-channel)
			checkConfigtxgen
			fetchRequiredChannelArtifacts
			createChannelArtifactsDir
            generateChannelArtifacts $CHANNEL_NAME
			moveIncrementChannelArtifacts
        ;;
        merge)
            mergeArtifactsCryptoDir
        ;;
		copy)
            copyArtifactsCryptoDir
        ;;
        regen)
            cleanChannelArtifacts
            
			checkConfigtxgen
            createChannelArtifactsDir
            generateCerts
			replacePrivateKey
			generateGenesisBlock

			generateChannelArtifacts $CHANNEL_NAME
        ;;
        *)
            usageHelp
            exit 1
        ;;
    esac        
done
