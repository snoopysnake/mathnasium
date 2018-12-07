@echo var > js/filelist.txt
@echo filelist =^" >> js/filelist.txt
dir "A:\Github\mathnasium\test" /b /s >> js/filelist.txt
@echo .^"; >> js/filelist.txt

setlocal EnableDelayedExpansion
break>js/filelist.js
@echo off
for /f "delims=" %%x in (js/filelist.txt) do (
	set "x=%%x"
  	set "x=!x:\=/!"
  	set "x=!x:'=\'!"
  	set "x=!x! "
	<nul set /p =!x!>> js/filelist.js
)

rem start index.html