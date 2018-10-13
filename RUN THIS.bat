@echo var > js/filelist.txt
@echo filelist='>> js/filelist.txt
dir "A:\Github\mathnasium\test" /b /s >> js/filelist.txt
@echo '; >> js/filelist.txt

break>js/filelist.js
@echo off
setlocal EnableDelayedExpansion
for /f "delims=" %%x in (js/filelist.txt) do (
	set "x=%%x"
  	set "x=!x:\=/!"
	echo|set /p=!x! >> js/filelist.js
)

start index.html