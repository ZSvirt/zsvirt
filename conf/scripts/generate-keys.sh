#!/bin/sh

OUTDIR=certs
mkdir -p $OUTDIR
TMP=`mktemp -t`
trap "rm $TMP* 2>/dev/null" 0
activation_date=`date -d "2 day ago" +"%Y-%m-%d %H:%M:%S"`

# DO NOT overwrite the generated certificates.
test -s $OUTDIR/privkey.pem && \
    { echo "certificates already generated"; exit 0; }

certtool --generate-privkey --rsa --bits=2048 --outfile $OUTDIR/privkey.pem
chmod 400 $OUTDIR/privkey.pem

# Generate the public key
# openssl rsa -in $OUTDIR/privkey.pem -pubout -out $OUTDIR/pubkey.pem
# certtool --generate-privkey --outfile $OUTDIR/pubkey.pem

# Generate the CA certificate
#
# The CN "store.zstack.org" will be used by the client to authenticate
# the server name, do NOT modify it.
# Use certtool instead of openssl
echo "organization = zstack"            > $TMP
echo "country = CN"                     >> $TMP
echo "state = Shanghai"                 >> $TMP
echo "locality = Shanghai"              >> $TMP
echo "cn = store.zstack.org"            >> $TMP
echo "dns_name = store.zstack.org"      >> $TMP
echo "expiration_days = 3652"           >> $TMP
echo "activation_date = \"$activation_date\"" >> $TMP
certtool --template "$TMP" \
         --generate-self-signed \
         --load-privkey "$OUTDIR/privkey.pem" \
         --outfile="$OUTDIR/ca.pem"

exit 0
