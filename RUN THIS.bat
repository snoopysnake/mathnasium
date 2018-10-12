@echo var > js/filelist.js
@echo filelist='>> js/filelist.js
dir test /b /s >> js/filelist.js
@echo '; >> js/filelist.js

@echo off
setlocal EnableDelayedExpansion
set row=
for /f "delims=" %%x in (js/filelist.js) do set "row=!row! %%x"
>js/filelist.js echo %row%

echo %row:\=/% >js/filelist.js

start index.html