// dllmain.cpp : Defines the entry point for the DLL application.
#include <Windows.h>

#define UNUSED(var) ((void)(var))

BOOL APIENTRY DllMain
(HMODULE hModule, DWORD  ul_reason_for_call, LPVOID lpReserved){
	UNUSED(hModule);
	UNUSED(ul_reason_for_call);
	UNUSED(lpReserved);
    return TRUE;
}
