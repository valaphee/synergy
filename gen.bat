"C:\Program Files\Java\graalvm-ce-java17-22.0.0.2\bin\java.exe" -jar .\tool\protod.jar -i "C:\\Program Files (x86)\\Battle.net\\Battle.net.13401\\battle.net.dll" -o src\main\proto
mkdir build\generated\source\proto\main\java\
for /R "src\main\proto" %%f in (*.proto) do .\tool\protoc-3.20.0-win64\bin\protoc.exe -I=%cd%\src\main\proto --java_out=build\generated\source\proto\main\java\ "%%f"
