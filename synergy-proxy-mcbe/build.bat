@echo off
go build -buildmode=c-shared -o src/main/resources/signature.dll signature.go
