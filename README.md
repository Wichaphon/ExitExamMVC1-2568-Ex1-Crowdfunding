# ExitExamMVC1-2568-Ex1-Crowdfunding
66050386 นายวิชญ์พล บัวทอง 

-ลบ files ใน resource ทั้งหมดก่อนค่อยรันก็ได้ครับ จะได้เริ่มตามใน seed เลย ใน github ผมกดเล่นไปเยอะอยู่

## Run กรณี vscode run ไม่ติด
### Windows (CMD/PowerShell)
```cmd
rd /s /q out 2>nul
mkdir out
javac -d out -encoding UTF-8 src\model\*.java src\controller\*.java src\view\*.java Main.java
java -cp out Main
