mkdir build\generated\source\proto\main\java\
for /R "src\main\proto" %%f in (*.proto) do .\protoc-3.20.0-win64\bin\protoc.exe -I=%cd%\src\main\proto --java_out=build\generated\source\proto\main\java\ "%%f"
