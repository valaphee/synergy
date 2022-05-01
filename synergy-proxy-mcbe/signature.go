package main

import (
	"bytes"
	"C"
	"crypto/ecdsa"
	"crypto/rand"
	"crypto/sha256"
	"crypto/x509"
	"encoding/binary"
	"time"
)

//export GetPublicKeyXY
func GetPublicKeyXY(derKey []byte, x *[]byte, y *[]byte) {
	key, _ := x509.ParsePKCS8PrivateKey(derKey)
	*x = (key.(*ecdsa.PrivateKey)).PublicKey.X.Bytes()
	*y = (key.(*ecdsa.PrivateKey)).PublicKey.Y.Bytes()
}

//export GenerateSignature
func GenerateSignature(derKey, method, url, authorization, body []byte, result *[]byte) {
	currentTime := (time.Now().Unix() + 11644473600) * 10000000
	hash := sha256.New()
	buf := bytes.NewBuffer([]byte{0, 0, 0, 1, 0})
	_ = binary.Write(buf, binary.BigEndian, currentTime)
	buf.Write([]byte{0})
	hash.Write(buf.Bytes())
	hash.Write(method)
	hash.Write([]byte{0})
	hash.Write(url)
	hash.Write([]byte{0})
	hash.Write(authorization)
	hash.Write([]byte{0})
	hash.Write(body)
	hash.Write([]byte{0})
	key, _ := x509.ParsePKCS8PrivateKey(derKey)
	r, s, _ := ecdsa.Sign(rand.Reader, key.(*ecdsa.PrivateKey), hash.Sum(nil))
	buf = bytes.NewBuffer([]byte{0, 0, 0, 1})
	_ = binary.Write(buf, binary.BigEndian, currentTime)
	*result = append(buf.Bytes(), append(r.Bytes(), s.Bytes()...)...)
}

func main() {}
