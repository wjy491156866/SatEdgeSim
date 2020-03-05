@echo off&setlocal EnableDelayedExpansion

set a=1

for /f "delims=" %%i in ('dir /b *.csv') do (

	if not "%%~ni"=="%~n0" (if !a! LSS 10 (ren "%%i" "edge!a!.csv") else ren "%%i" "edge!a!.csv"

	set/a a+=1

	)
	
)