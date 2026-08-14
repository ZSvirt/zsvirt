#!/bin/bash

OUTDIR=spice-certs
mkdir -p $OUTDIR
CA_KEY=ca-key.pem
SERVER_KEY=server-key.pem

# creating a key for our ca
test -s $OUTDIR/$CA_KEY && test -s $OUTDIR/$SERVER_KEY && { echo "spice certificates already generated"; exit 0; }

openssl genrsa -out $OUTDIR/$CA_KEY 2048
chmod 400 $OUTDIR/$CA_KEY

# creating a ca
openssl req -new -x509 -days 3650 -key $OUTDIR/$CA_KEY -out $OUTDIR/ca-cert.pem -utf8 -subj "/C=CN/L=Shanghai/O=ZStack/CN=my CA"

# create server key
openssl genrsa -out $OUTDIR/$SERVER_KEY 2048
chmod 400 $OUTDIR/$SERVER_KEY

# create a certificate signing request (csr)
openssl req -new -key $OUTDIR/$SERVER_KEY -out $OUTDIR/server-key.csr -utf8 -subj "/C=CN/L=Shanghai/O=ZStack/CN=spice.zstack.org"

# signing our server certificate with this ca
openssl x509 -req -days 3650 -in $OUTDIR/server-key.csr -CA $OUTDIR/ca-cert.pem -CAkey $OUTDIR/$CA_KEY -set_serial 01 -out $OUTDIR/server-cert.pem

exit 0